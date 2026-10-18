package pl.edu.agh.backend.notifications;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Map;

public record NotificationPayload(
        @Schema(requiredMode = RequiredMode.REQUIRED) NotificationType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) String targetSlug) {

    Map<String, String> toData() {
        return Map.of("type", type.name(), "targetSlug", targetSlug);
    }
}
