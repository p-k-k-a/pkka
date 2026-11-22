export const MESSAGE_LIMITS = {
  notificationTitle: 100,
  notification: 150,
  subject: 150,
  body: 5000,
} as const;

const MAIL_DATE = new Intl.DateTimeFormat("pl-PL", {
  day: "numeric",
  month: "long",
  year: "numeric",
  timeZone: "Europe/Warsaw",
});

const MAIL_TIME = new Intl.DateTimeFormat("pl-PL", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Europe/Warsaw",
});

export function formatMailStartsAt(iso: string) {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "";
  return `${MAIL_DATE.format(date)} o ${MAIL_TIME.format(date)}`;
}
