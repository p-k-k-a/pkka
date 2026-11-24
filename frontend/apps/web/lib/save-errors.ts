import { ApiError } from "@pkka/api";

/**
 * The API answers 503 when a record that has an uploaded file attached is saved while the
 * object storage is not configured (it refuses rather than silently detaching the file), or
 * when a newly uploaded file cannot be checked because the storage does not answer.
 */
export function isStorageUnavailable(error: unknown) {
  return error instanceof ApiError && error.status === 503;
}

const POST_CONTENT_REASONS = {
  POST_CONTENT_LINK: "link",
  POST_CONTENT_PICTURE: "picture",
} as const;

/**
 * Why the API refused a post because of its content, or null for any other error:
 * - `link`: a link to a stored file that is not a post picture (a material, a cover). Only
 *   pictures inserted with „Wstaw zdjęcie” can be placed in a post.
 * - `picture`: a picture that never reached the storage.
 */
export function postContentRejection(error: unknown) {
  if (!(error instanceof ApiError) || error.status !== 400) return null;
  const reason = (error.body as { reason?: unknown } | null)?.reason;
  return typeof reason === "string" && reason in POST_CONTENT_REASONS
    ? POST_CONTENT_REASONS[reason as keyof typeof POST_CONTENT_REASONS]
    : null;
}

/** The message for a post refused because of its content; null for any other error. */
export function postContentRejectionMessage(error: unknown) {
  switch (postContentRejection(error)) {
    case "link":
      return "W treści jest link do pliku z materiałów albo okładki. We wpisie mogą być tylko zdjęcia wstawione przyciskiem „Wstaw zdjęcie”. Usuń ten link.";
    case "picture":
      return "Jedno ze zdjęć w treści nie zostało przesłane. Usuń je i wstaw ponownie przyciskiem „Wstaw zdjęcie”.";
    default:
      return null;
  }
}
