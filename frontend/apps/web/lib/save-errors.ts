import { ApiError } from "@pkka/api";

/**
 * The API answers 503 when a record that has an uploaded file attached is saved while the
 * object storage is not configured: it refuses rather than silently detaching the file.
 */
export function isStorageUnavailable(error: unknown) {
  return error instanceof ApiError && error.status === 503;
}

/**
 * Whether the API refused a post because of something in its content: a link to a stored file
 * that would stop working within the hour, or a picture that was never uploaded. The backend
 * starts the `detail` of exactly these refusals with "content".
 */
export function isPostContentRejected(error: unknown) {
  if (!(error instanceof ApiError) || error.status !== 400) return false;
  const detail = (error.body as { detail?: unknown } | null)?.detail;
  return typeof detail === "string" && detail.startsWith("content ");
}
