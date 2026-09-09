/**
 * Mirrors `pl.edu.agh.backend.notifications.NotificationType`. It only ever travels inside a push payload,
 * never through a controller, so it is absent from the OpenAPI spec and cannot be generated — keep the two
 * lists in step by hand.
 */
export const NOTIFICATION_TYPES = ["EVENT_ANNOUNCEMENT", "EVENT_REMINDER"] as const;

export type NotificationType = (typeof NOTIFICATION_TYPES)[number];

export type NotificationPayload = {
  type: NotificationType;
  targetId: string;
};

/** Push data is untrusted input, and an older build must ignore a type it has never heard of rather than crash. */
export function parseNotificationPayload(data: unknown): NotificationPayload | null {
  if (typeof data !== "object" || data === null) return null;

  const { type, targetId } = data as Record<string, unknown>;
  if (typeof type !== "string" || typeof targetId !== "string" || targetId === "") return null;
  if (!NOTIFICATION_TYPES.includes(type as NotificationType)) return null;

  return { type: type as NotificationType, targetId };
}
