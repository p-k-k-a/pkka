export function formatPublishedAt(publishedAt?: string) {
  if (!publishedAt) {
    return { dateLabel: "Nieznana data", timeLabel: "--:--" };
  }

  const date = new Date(publishedAt);
  if (Number.isNaN(date.getTime())) {
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
