package pl.edu.agh.backend.notifications;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(enumAsRef = true)
@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EVENT_ANNOUNCEMENT("events"),
    EVENT_REMINDER("reminders");

    /** Android drops a notification whose channel does not exist on the device. */
    private final String channelId;
}
