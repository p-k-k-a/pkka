import { MaterialType } from "@pkka/api";

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

// Exactly the backend's @Pattern on MaterialRequest.url. URL parsing would be laxer
// (it accepts "HTTPS://…" or "https:example.com"), letting the form save a link the
// API then rejects with a 400.
const MATERIAL_URL_PATTERN = /^https?:\/\/.+/;

/** Same rule as the backend: an http(s) URL within the length limit. */
export function isValidMaterialUrl(url: string) {
  const trimmed = url.trim();
  return trimmed.length <= MATERIAL_URL_MAX && MATERIAL_URL_PATTERN.test(trimmed);
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

/** "1 materiał", "3 materiały", "5 materiałów" — Polish plural forms. */
export function formatMaterialCount(count: number) {
  const lastDigit = count % 10;
  const lastTwo = count % 100;
  const noun =
    count === 1
      ? "materiał"
      : lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)
        ? "materiały"
        : "materiałów";
  return `${count} ${noun}`;
}
