import { ApiError } from "./mutator";
import type { PresignedUploadResponse } from "./src/generated/schemas";

/** What the backend accepts for one kind of file; checked here first to spare a round trip. */
export type UploadRule = {
  maxBytes: number;
  /** Exact MIME types, lower-case, without parameters. */
  contentTypes: readonly string[];
};

export type UploadErrorReason =
  | "unsupported-type"
  | "too-large"
  | "empty"
  /** Object storage is not configured on the backend. */
  | "unavailable"
  | "network"
  | "aborted"
  | "failed";

export class UploadError extends Error {
  constructor(public reason: UploadErrorReason) {
    super(`upload failed: ${reason}`);
    this.name = "UploadError";
  }
}

type PresignRequest = { contentType: string; sizeBytes: number };

/** One of the generated presign calls, e.g. `presignAvatar`. */
export type PresignUpload = (
  request: PresignRequest,
) => Promise<{ status: number; data: PresignedUploadResponse | void }>;

export type UploadInput = {
  /** A `File` on the web; on React Native the blob read from the picked file's URI. */
  blob: Blob;
  /** Defaults to `blob.type`. React Native blobs do not always carry one. */
  contentType?: string;
};

type UploadOptions = UploadInput & {
  rule: UploadRule;
  presign: PresignUpload;
  /** Called with the uploaded fraction, 0 to 1. */
  onProgress?: (fraction: number) => void;
  signal?: AbortSignal;
};

/** Why `rule` rejects the file, or `null` when it is fine to upload. */
export function uploadRejection(
  rule: UploadRule,
  contentType: string,
  sizeBytes: number,
): UploadErrorReason | null {
  if (!rule.contentTypes.includes(contentType)) return "unsupported-type";
  if (sizeBytes <= 0) return "empty";
  if (sizeBytes > rule.maxBytes) return "too-large";
  return null;
}

function presignFailure(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 413) return new UploadError("too-large");
    if (error.status === 415) return new UploadError("unsupported-type");
    if (error.status === 503) return new UploadError("unavailable");
    if (error.status === 0) return new UploadError("network");
  }
  return new UploadError("failed");
}

// XMLHttpRequest rather than fetch: it reports upload progress, in browsers and in React Native.
// Deliberately not apiFetch either: the URL is signed for exactly the returned headers, and
// cookies, a bearer token or a CSRF header have no business going to the storage host.
function putToStorage(
  presigned: PresignedUploadResponse,
  blob: Blob,
  onProgress?: (fraction: number) => void,
  signal?: AbortSignal,
) {
  return new Promise<void>((resolve, reject) => {
    const request = new XMLHttpRequest();
    const abort = () => request.abort();
    const settle = (error?: UploadError) => {
      signal?.removeEventListener("abort", abort);
      if (error) reject(error);
      else resolve();
    };

    request.open("PUT", presigned.uploadUrl);
    for (const [name, value] of Object.entries(presigned.headers)) {
      request.setRequestHeader(name, value);
    }
    request.upload.onprogress = (progressEvent) => {
      if (progressEvent.lengthComputable && progressEvent.total > 0) {
        onProgress?.(progressEvent.loaded / progressEvent.total);
      }
    };
    request.onload = () => {
      const ok = request.status >= 200 && request.status < 300;
      settle(ok ? undefined : new UploadError("failed"));
    };
    request.onerror = () => settle(new UploadError("network"));
    request.onabort = () => settle(new UploadError("aborted"));

    signal?.addEventListener("abort", abort);
    request.send(blob);
  });
}

/**
 * Uploads a file straight to object storage: asks the backend for a presigned URL, PUTs the
 * bytes there, and resolves with the `objectKey` to send back where the file is attached
 * (`avatarKey`, an event's `coverImageUrl`, a material's `url`). The bucket is private, so
 * there is no public URL: read endpoints return a short-lived signed one for the stored key.
 *
 * @throws UploadError, never anything else
 */
export async function uploadFile({
  blob,
  contentType = blob.type,
  rule,
  presign,
  onProgress,
  signal,
}: UploadOptions): Promise<string> {
  const type = contentType.trim().toLowerCase();
  const rejection = uploadRejection(rule, type, blob.size);
  if (rejection) throw new UploadError(rejection);
  if (signal?.aborted) throw new UploadError("aborted");

  let presigned: PresignedUploadResponse;
  try {
    const response = await presign({ contentType: type, sizeBytes: blob.size });
    if (response.status !== 200 || !response.data) throw new UploadError("failed");
    presigned = response.data;
  } catch (error) {
    throw error instanceof UploadError ? error : presignFailure(error);
  }
  if (signal?.aborted) throw new UploadError("aborted");

  await putToStorage(presigned, blob, onProgress, signal);
  return presigned.objectKey;
}
