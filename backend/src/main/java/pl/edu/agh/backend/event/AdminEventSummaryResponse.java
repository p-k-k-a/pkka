package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminEventSummaryResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        String location,
        @Schema(requiredMode = RequiredMode.REQUIRED) Audience audience,
        String coverImageUrl,
        Integer seatLimit) {

    static AdminEventSummaryResponse from(Event event) {
        return new AdminEventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getType(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getLocation(),
                event.getAudience(),
                event.getCoverImageUrl(),
                event.getSeatLimit());
    }
}
