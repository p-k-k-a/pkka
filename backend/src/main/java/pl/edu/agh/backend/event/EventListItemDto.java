package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record EventListItemDto(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        String location,
        String coverImageUrl,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED) Integer seatsTaken) {
    public static EventListItemDto from(Event e) {
        return new EventListItemDto(
                e.getId(),
                e.getTitle(),
                e.getStartsAt(),
                e.getType(),
                e.getLocation(),
                e.getCoverImageUrl(),
                e.getSeatLimit(),
                0 // TODO:Change after adding EventRegistration
                );
    }
}
