import { clsx, type ClassValue } from "clsx";
import { format, isValid, parseISO } from "date-fns";
import { pl } from "date-fns/locale";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function parseDate(value?: string) {
  if (!value) return null;
  const date = parseISO(value);
  return isValid(date) ? date : null;
}

export function formatPublishedAt(publishedAt?: string) {
  const date = parseDate(publishedAt);
  if (!date) return "";
  return format(date, "dd.MM.yyyy - HH:mm", { locale: pl });
}

export function formatEventDateLong(startsAt?: string) {
  const date = parseDate(startsAt);
  if (!date) return "";
  return format(date, "d MMMM yyyy", { locale: pl });
}

export function formatEventDateShort(startsAt?: string) {
  const date = parseDate(startsAt);
  if (!date) return "";
  return format(date, "d MMM yyyy", { locale: pl });
}

export function formatTimeRange(startsAt?: string, endsAt?: string) {
  const start = parseDate(startsAt);
  if (!start) return "";
  const end = parseDate(endsAt);
  return end ? `${format(start, "HH:mm")} - ${format(end, "HH:mm")}` : format(start, "HH:mm");
}
