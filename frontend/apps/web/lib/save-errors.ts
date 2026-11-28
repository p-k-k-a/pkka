import { ApiError } from "@pkka/api";

/**
 * The API answers 503 when a record that has an uploaded file attached is saved while the
 * object storage is not configured: it refuses rather than silently detaching the file.
 */
export function isStorageUnavailable(error: unknown) {
  return error instanceof ApiError && error.status === 503;
}

/**
 * Whether the API refused a post because of a picture in its content, e.g. a link to a stored
 * file that would stop working within the hour. The backend marks exactly these refusals with
 * `reason: "POST_CONTENT"`.
 */
export function isPostContentRejected(error: unknown) {
  if (!(error instanceof ApiError) || error.status !== 400) return false;
  return (error.body as { reason?: unknown } | null)?.reason === "POST_CONTENT";
}
