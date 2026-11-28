import { Audience, MaterialType } from "@pkka/api";
import { pluralPl } from "../format/plural";

// Mirrors the constraints on the backend's MaterialRequest.
export const MATERIAL_TITLE_MAX = 300;
export const MATERIAL_URL_MAX = 2000;

export const MATERIALS_PAGE_SIZE = 20;

const MATERIAL_TYPE_LABELS: Record<MaterialType, string> = {
  RECORDING: "Nagranie",
  PRESENTATION: "Prezentacja",
  OTHER: "Inne",
};

export function materialTypeLabel(type: MaterialType) {
  return MATERIAL_TYPE_LABELS[type];
}

export function materialTypeLabelUpper(type: MaterialType) {
  return MATERIAL_TYPE_LABELS[type].toUpperCase();
}

export const MATERIAL_TYPE_OPTIONS = Object.values(MaterialType).map((value) => ({
  value,
  label: MATERIAL_TYPE_LABELS[value],
}));

// Who sees a material that has no event. Worded for materials: nothing here is open to
// people who are not signed in, so the event label "Publiczne" would promise too much.
export const MATERIAL_AUDIENCE_OPTIONS: { value: Audience; label: string }[] = [
  { value: Audience.ALL_ALUMNI, label: "Tylko zweryfikowani alumni" },
  { value: Audience.PUBLIC, label: "Wszyscy zalogowani" },
];

export function isMaterialAudience(value: unknown): value is Audience {
  return MATERIAL_AUDIENCE_OPTIONS.some((option) => option.value === value);
}

export function isMaterialType(value: unknown): value is MaterialType {
  return Object.values(MaterialType).includes(value as MaterialType);
}

function parseUrl(url: string) {
  try {
    return new URL(url);
  } catch {
    return null;
  }
}

// The backend's @Pattern on MaterialRequest.url: a link, or the object key of an uploaded
// file. `new URL()` would be laxer and let the form save a link the API rejects.
const MATERIAL_URL_PATTERN = /^(https?:\/\/|materials\/).+/i;

export function isValidMaterialUrl(url: string) {
  const trimmed = url.trim();
  return trimmed.length <= MATERIAL_URL_MAX && MATERIAL_URL_PATTERN.test(trimmed);
}

const UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
// The keys the backend issues for materials: materials/<uuid>.<ext>.
const UPLOADED_MATERIAL_KEY = new RegExp(`^materials/${UUID}\\.([a-z0-9]+)$`);

/**
 * The file extension when the form holds the object key of an uploaded file, null for a link.
 * Saved materials need no guessing: the API says it in `fileExtension` and `fileKey`.
 */
export function uploadedMaterialExtension(value: string) {
  return value.trim().match(UPLOADED_MATERIAL_KEY)?.[1] ?? null;
}

const KNOWN_HOSTS: { suffix: string; label: string }[] = [
  { suffix: "youtube.com", label: "YouTube" },
  { suffix: "youtu.be", label: "YouTube" },
  { suffix: "drive.google.com", label: "Google Drive" },
  { suffix: "docs.google.com", label: "Google Docs" },
  { suffix: "github.com", label: "GitHub" },
  { suffix: "vimeo.com", label: "Vimeo" },
];

function hostMatches(hostname: string, suffix: string) {
  return hostname === suffix || hostname.endsWith(`.${suffix}`);
}

/** A short name for where the link leads, e.g. "YouTube" or "example.com". */
export function materialLinkHost(url: string) {
  const parsed = parseUrl(url);
  if (!parsed) return null;
  const hostname = parsed.hostname.replace(/^www\./, "");
  return KNOWN_HOSTS.find(({ suffix }) => hostMatches(hostname, suffix))?.label ?? hostname;
}

const YOUTUBE_ID = /^[\w-]{11}$/;

/** The video id of a YouTube watch, short, shorts or embed link; null for anything else. */
export function youtubeVideoId(url: string) {
  const parsed = parseUrl(url);
  if (!parsed) return null;
  const hostname = parsed.hostname.replace(/^(www|m)\./, "");
  const segments = parsed.pathname.split("/").filter(Boolean);

  let id: string | null | undefined = null;
  if (hostname === "youtu.be") {
    id = segments[0];
  } else if (hostMatches(hostname, "youtube.com")) {
    id =
      segments[0] === "watch"
        ? parsed.searchParams.get("v")
        : ["shorts", "embed", "live"].includes(segments[0] ?? "")
          ? segments[1]
          : null;
  }

  return id && YOUTUBE_ID.test(id) ? id : null;
}

export function youtubeThumbnailUrl(url: string) {
  const id = youtubeVideoId(url);
  return id ? `https://img.youtube.com/vi/${id}/hqdefault.jpg` : null;
}

export function formatMaterialCount(count: number) {
  return `${count} ${pluralPl(count, "materiał", "materiały", "materiałów")}`;
}
