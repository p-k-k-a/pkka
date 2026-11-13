package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminEventSummaryResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        String location,
        @Schema(requiredMode = RequiredMode.REQUIRED) Audience audience,

        @Schema(description = "Signed, short-lived URL when the image is an uploaded file, otherwise the external URL")
        String coverImageUrl,

        Integer seatLimit,

        @Schema(description = "Admin who created the event; absent for events that predate authorship tracking")
        String authorDisplayName) {

    static AdminEventSummaryResponse from(Event event, String coverImageUrl) {
        return new AdminEventSummaryResponse(
                event.getId(),
                event.getSlug(),
                event.getTitle(),
                event.getType(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getLocation(),
                event.getAudience(),
                coverImageUrl,
                event.getSeatLimit(),
                event.getAuthor() == null ? null : event.getAuthor().getDisplayName());
    }
}
