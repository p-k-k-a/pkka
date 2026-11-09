type CalendarEvent = {
  title: string;
  startsAt: string;
  endsAt: string;
  location?: string;
  details?: string;
};

function toGoogleStamp(iso: string) {
  const time = Date.parse(iso);
  if (Number.isNaN(time)) return null;
  return new Date(time).toISOString().replace(/[-:]|\.\d{3}/g, "");
}

export function googleCalendarUrl(event: CalendarEvent) {
  const start = toGoogleStamp(event.startsAt);
  const end = toGoogleStamp(event.endsAt);
  if (!start || !end) return null;

  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: event.title,
    dates: `${start}/${end}`,
  });
  const location = event.location?.trim();
  if (location) params.set("location", location);
  if (event.details) params.set("details", event.details);

  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}
