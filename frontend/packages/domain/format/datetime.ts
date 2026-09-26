import { format, isValid, parseISO } from "date-fns";
import { pl } from "date-fns/locale";

const UNKNOWN_DATE = "Nieznana data";
const UNKNOWN_TIME = "--:--";

function parseDate(iso?: string) {
  if (!iso) return null;
  const date = parseISO(iso);
  return isValid(date) ? date : null;
}

export function formatPublishedAt(publishedAt?: string) {
  const date = parseDate(publishedAt);
  if (!date) {
    return { dateLabel: UNKNOWN_DATE, timeLabel: UNKNOWN_TIME };
  }

  return {
    dateLabel: format(date, "dd MMMM yyyy", { locale: pl }),
    timeLabel: format(date, "HH:mm", { locale: pl }),
  };
}

export function formatPublishedAtCompact(publishedAt?: string) {
  const date = parseDate(publishedAt);
  if (!date) return UNKNOWN_DATE;
  return format(date, "dd.MM.yyyy - HH:mm", { locale: pl });
}

export function formatDateTime(value?: string) {
  if (!value) return "—";
  const { dateLabel, timeLabel } = formatPublishedAt(value);
  return `${dateLabel}, ${timeLabel}`;
}

export function formatEventDateComma(iso: string) {
  const date = parseDate(iso);
  if (!date) return UNKNOWN_DATE;
  return format(date, "d MMMM, yyyy", { locale: pl });
}

export function formatEventDateLong(iso: string) {
  const date = parseDate(iso);
  if (!date) return UNKNOWN_DATE;
  return format(date, "d MMMM yyyy", { locale: pl });
}

export function formatEventDateShort(iso: string) {
  const date = parseDate(iso);
  if (!date) return UNKNOWN_DATE;
  return format(date, "d MMM yyyy", { locale: pl });
}

export function formatTimeRange(startsAt: string, endsAt: string) {
  const start = parseDate(startsAt);
  if (!start) return UNKNOWN_TIME;

  const startLabel = format(start, "HH:mm", { locale: pl });
  const end = parseDate(endsAt);
  return end ? `${startLabel} – ${format(end, "HH:mm", { locale: pl })}` : startLabel;
}

export function isEventPast(startsAt?: string) {
  const date = parseDate(startsAt);
  return date != null && date.getTime() <= Date.now();
}

// The value shape of an <input type="datetime-local">: local wall-clock time, no zone.
// Only web has such an input today, but keeping it here means one parser and one
// formatting engine for every date in the frontend.
export function toDatetimeLocalValue(iso?: string) {
  const date = parseDate(iso);
  if (!date) return "";
  return format(date, "yyyy-MM-dd'T'HH:mm");
}

export function fromDatetimeLocalValue(value: string) {
  const date = parseDate(value);
  return date ? date.toISOString() : undefined;
}
