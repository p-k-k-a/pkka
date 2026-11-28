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

/** Event covers, post covers and pictures inside a post: the backend allows them the same. */
export const IMAGE_UPLOAD: LabelledUploadRule = {
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

const CONTENT_TYPES_BY_EXTENSION: Record<string, string> = {
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  webp: "image/webp",
  pdf: "application/pdf",
  doc: "application/msword",
  docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  ppt: "application/vnd.ms-powerpoint",
  pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
  xlsx: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  txt: "text/plain",
  mp4: "video/mp4",
  webm: "video/webm",
};

/**
 * The MIME type to upload a picked file as. A browser leaves `type` empty when the system has
 * no program registered for the extension (an Office file on a machine without Office), so
 * the extension decides then. Empty when neither is known; the upload rule rejects that.
 */
export function fileContentType(file: { name: string; type: string }) {
  if (file.type) return file.type;
  const extension = file.name.split(".").pop()?.toLowerCase() ?? "";
  return CONTENT_TYPES_BY_EXTENSION[extension] ?? "";
}

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
    case "too-many":
      return "Przesłano już za dużo plików. Spróbuj ponownie za kilka godzin.";
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

// The object keys of pictures uploaded for an event or a post.
const STORED_PICTURE_KEY = "(?:covers/(?:events|posts)|posts/images)/[0-9a-f-]{36}\\.[a-z0-9]+";
const STORED_PICTURE = new RegExp(`^${STORED_PICTURE_KEY}$`);
const STORED_PICTURE_IN_CONTENT = new RegExp(`\\]\\(${STORED_PICTURE_KEY}\\)`);

/**
 * Whether the API sent an uploaded picture as its bare object key instead of a signed URL: it
 * can only do that when it cannot sign, i.e. the file storage is not configured. The browser
 * cannot show such a picture, but sending it back unchanged keeps it attached.
 */
export function isUnsignedPicture(value: string | null | undefined) {
  return value != null && STORED_PICTURE.test(value.trim());
}

/** Whether Markdown holds an uploaded picture as its bare object key; see `isUnsignedPicture`. */
export function hasUnsignedPictures(markdown: string | null | undefined) {
  return markdown != null && STORED_PICTURE_IN_CONTENT.test(markdown);
}
