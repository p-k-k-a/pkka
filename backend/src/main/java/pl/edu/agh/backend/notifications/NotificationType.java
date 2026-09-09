package pl.edu.agh.backend.notifications;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The discriminator the mobile app switches on to pick a route. It never appears in a controller signature, so
 * it is absent from the OpenAPI spec — {@code frontend/packages/domain/notifications.ts} mirrors it by hand.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EVENT_ANNOUNCEMENT("events"),
    EVENT_REMINDER("reminders");

    /** Android drops a notification whose channel does not exist on the device. */
    private final String channelId;
}
