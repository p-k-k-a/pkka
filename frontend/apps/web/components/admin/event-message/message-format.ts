import { format, isValid, parseISO } from "date-fns";
import { pl } from "date-fns/locale";

export const MESSAGE_LIMITS = {
  notification: 150,
  subject: 150,
  body: 5000,
} as const;

export function formatMailStartsAt(iso: string) {
  const date = parseISO(iso);
  return isValid(date) ? format(date, "d MMMM yyyy 'o' HH:mm", { locale: pl }) : "";
}
