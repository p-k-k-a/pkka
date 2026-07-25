import { formatPublishedAt } from "@/lib/format-published-at";

export function formatDateTime(value?: string) {
  if (!value) return "—";
  const { dateLabel, timeLabel } = formatPublishedAt(value);
  return `${dateLabel}, ${timeLabel}`;
}
