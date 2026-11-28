import { MaterialType } from "@pkka/api";
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
  const toSave = materialUrlToSave(url);
  return toSave.length <= MATERIAL_URL_MAX && MATERIAL_URL_PATTERN.test(toSave);
}

const UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
// The keys the backend issues for materials: materials/<uuid>.<ext>.
const UPLOADED_MATERIAL_KEY = new RegExp(`^materials/${UUID}\\.([a-z0-9]+)$`);
const UPLOADED_MATERIAL_URL_PATH = new RegExp(`/materials/${UUID}\\.([a-z0-9]+)$`);
// Signed download URLs point at the club's object storage (Cloudflare R2).
const STORAGE_HOST_SUFFIX = ".r2.cloudflarestorage.com";

function isHttpUrl(value: string) {
  return /^https?:\/\//i.test(value);
}

/**
 * The file extension when the material is a file uploaded to our storage, null for a link.
 * An upload shows up either as its object key (just uploaded, not saved yet) or as the
 * signed download URL the API returns for it. A link to another site is never taken for
 * an upload, even when its path happens to look like one of our keys.
 */
export function uploadedMaterialExtension(url: string) {
  const trimmed = url.trim();
  if (!isHttpUrl(trimmed)) return trimmed.match(UPLOADED_MATERIAL_KEY)?.[1] ?? null;

  const parsed = parseUrl(trimmed);
  if (!parsed?.hostname.endsWith(STORAGE_HOST_SUFFIX)) return null;
  return parsed.pathname.match(UPLOADED_MATERIAL_URL_PATH)?.[1] ?? null;
}

/**
 * What to send as `MaterialRequest.url`. The signed URL of an uploaded file goes back without
 * its query: the backend recognises the file by host and path alone, and the signature (which
 * carries the download name) can push the URL past the length limit.
 */
export function materialUrlToSave(url: string) {
  const trimmed = url.trim();
  if (!isHttpUrl(trimmed) || uploadedMaterialExtension(trimmed) === null) return trimmed;

  const parsed = parseUrl(trimmed);
  return parsed ? `${parsed.origin}${parsed.pathname}` : trimmed;
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
