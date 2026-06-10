function parseDate(iso?: string) {
  if (!iso) return null;
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatEventDateTime(iso?: string) {
  const date = parseDate(iso);
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
  const start = formatEventDateTime(startsAt);
  if (!endsAt) return start.timeLabel;

  const end = formatEventDateTime(endsAt);
  return `${start.timeLabel} - ${end.timeLabel}`;
}

export function formatEventDateTimeRange(startsAt?: string, endsAt?: string) {
  const start = formatEventDateTime(startsAt);
  if (!endsAt) {
    return `${start.dateLabel}, ${start.timeLabel}`;
  }

  const end = formatEventDateTime(endsAt);
  if (start.dateLabel === end.dateLabel) {
    return `${start.dateLabel}, ${start.timeLabel} – ${end.timeLabel}`;
  }

  return `${start.dateLabel}, ${start.timeLabel} – ${end.dateLabel}, ${end.timeLabel}`;
}
