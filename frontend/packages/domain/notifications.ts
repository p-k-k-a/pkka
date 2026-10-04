import { NotificationType, type NotificationPayload } from "@pkka/api";

const NOTIFICATION_TYPES: readonly string[] = Object.values(NotificationType);

export function parseNotificationPayload(data: unknown): NotificationPayload | null {
  if (typeof data !== "object" || data === null) return null;

  const { type, targetId } = data as Record<string, unknown>;
  if (typeof type !== "string" || typeof targetId !== "string" || targetId === "") return null;
  if (!NOTIFICATION_TYPES.includes(type)) return null;

  return { type: type as NotificationType, targetId };
}
