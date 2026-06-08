export function formatEventDateTime(iso?: string) {
  if (!iso) {
    return { dateLabel: "Nieznana data", timeLabel: "--:--" };
  }

  const date = new Date(iso);
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
