function parseDate(iso?: string) {
  if (!iso) return null;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

/**
 * Structured publication stamp. Callers render `dateLabel` alone, or both parts,
 * so the two halves stay separate rather than pre-joined.
 */
export function formatPublishedAt(publishedAt?: string) {
  const date = parseDate(publishedAt);
  if (!date) {
    return { dateLabel: "Nieznana data", timeLabel: "--:--" };
  }

  const dateLabel = new Intl.DateTimeFormat("pl-PL", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(date);

  const timeLabel = new Intl.DateTimeFormat("pl-PL", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);

  return { dateLabel, timeLabel };
}

export function formatDateTime(value?: string) {
  if (!value) return "—";
  const { dateLabel, timeLabel } = formatPublishedAt(value);
  return `${dateLabel}, ${timeLabel}`;
}

export function formatEventDateLong(iso?: string) {
  const date = parseDate(iso);
  if (!date) return "Nieznana data";

  return new Intl.DateTimeFormat("pl-PL", {
    day: "numeric",
    month: "long",
    year: "numeric",
  }).format(date);
}

export function formatEventDateShort(iso?: string) {
  const date = parseDate(iso);
  if (!date) return "Nieznana data";

  return new Intl.DateTimeFormat("pl-PL", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(date);
}

export function formatTimeRange(startsAt?: string, endsAt?: string) {
  const start = formatPublishedAt(startsAt);
  if (!endsAt) return start.timeLabel;

  const end = formatPublishedAt(endsAt);
  return `${start.timeLabel} - ${end.timeLabel}`;
}
