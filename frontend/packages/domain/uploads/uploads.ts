import { UploadError, type UploadRule } from "@pkka/api";

const MB = 1024 * 1024;

const IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"];
const IMAGE_FORMATS = "JPG, PNG lub WebP";

export type LabelledUploadRule = UploadRule & {
  /** The accepted formats as a user would name them, e.g. "JPG, PNG lub WebP". */
  formats: string;
};

// Mirrors the backend's UploadCategory: the presign endpoint rejects anything else.
export const AVATAR_UPLOAD: LabelledUploadRule = {
  maxBytes: 2 * MB,
  contentTypes: IMAGE_TYPES,
  formats: IMAGE_FORMATS,
};

export const EVENT_COVER_UPLOAD: LabelledUploadRule = {
  maxBytes: 5 * MB,
  contentTypes: IMAGE_TYPES,
  formats: IMAGE_FORMATS,
};

export const MATERIAL_UPLOAD: LabelledUploadRule = {
  maxBytes: 100 * MB,
  contentTypes: [
    ...IMAGE_TYPES,
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain",
    "video/mp4",
    "video/webm",
  ],
  formats: "PDF, DOC, DOCX, PPT, PPTX, XLSX, TXT, JPG, PNG, WebP, MP4 lub WebM",
};

/** "2 MB", "100 MB": the limits are whole megabytes. */
export function uploadLimitLabel(rule: UploadRule) {
  return `${Math.round(rule.maxBytes / MB)} MB`;
}

/** What to tell the user when an upload under `rule` failed. Null when they cancelled it themselves. */
export function uploadErrorMessage(error: unknown, rule: LabelledUploadRule) {
  const reason = error instanceof UploadError ? error.reason : "failed";
  switch (reason) {
    case "aborted":
      return null;
    case "unsupported-type":
      return `Ten format pliku nie jest obsługiwany. Wybierz plik ${rule.formats}.`;
    case "too-large":
      return `Plik jest za duży. Maksymalny rozmiar to ${uploadLimitLabel(rule)}.`;
    case "empty":
      return "Ten plik jest pusty. Wybierz inny.";
    case "unavailable":
      return "Przesyłanie plików jest chwilowo niedostępne. Spróbuj ponownie później.";
    case "network":
      return "Nie udało się przesłać pliku. Sprawdź połączenie z internetem i spróbuj ponownie.";
    case "failed":
      return "Nie udało się przesłać pliku. Spróbuj ponownie.";
  }
}

/** "42%" for the fraction a running upload reports. */
export function uploadProgressLabel(fraction: number) {
  return `${Math.round(Math.min(Math.max(fraction, 0), 1) * 100)}%`;
}
