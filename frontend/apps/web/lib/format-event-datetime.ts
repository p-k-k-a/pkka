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

export function formatEventDateComma(iso?: string) {
  const date = parseDate(iso);
  if (!date) return "Nieznana data";

  const day = new Intl.DateTimeFormat("pl-PL", { day: "numeric" }).format(date);
  const month = new Intl.DateTimeFormat("pl-PL", { month: "long" }).format(date);
  const year = new Intl.DateTimeFormat("pl-PL", { year: "numeric" }).format(date);
  return `${day} ${month}, ${year}`;
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
  return `${start.timeLabel} – ${end.timeLabel}`;
}

export function toDatetimeLocalValue(iso?: string) {
  const date = parseDate(iso);
  if (!date) return "";

  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function fromDatetimeLocalValue(value: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

export function isEventPast(startsAt?: string) {
  const date = parseDate(startsAt);
  return date != null && date.getTime() <= Date.now();
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
