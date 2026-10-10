package pl.edu.agh.backend.notifications;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Map;
import java.util.UUID;

public record NotificationPayload(
        @Schema(requiredMode = RequiredMode.REQUIRED) NotificationType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID targetId) {

    Map<String, String> toData() {
        return Map.of("type", type.name(), "targetId", targetId.toString());
    }
}
