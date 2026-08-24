package pl.edu.agh.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventType;

public record EventListItemResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        String location,
        String coverImageUrl,

        @Schema(description = "Absent when the event has no seat limit")
        Integer seatLimit,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Registrations for this event so far")
        int seatsTaken,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Whether the requesting user is signed up; false for anonymous callers")
        boolean registered) {
    public static EventListItemResponse from(Event e, long seatsTaken, boolean registered) {
        return new EventListItemResponse(
                e.getId(),
                e.getTitle(),
                e.getStartsAt(),
                e.getType(),
                e.getLocation(),
                e.getCoverImageUrl(),
                e.getSeatLimit(),
                (int) seatsTaken,
                registered);
    }
}
