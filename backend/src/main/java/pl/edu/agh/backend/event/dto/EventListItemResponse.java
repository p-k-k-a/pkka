package pl.edu.agh.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;

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

        @Schema(description = "The requesting user's own sign-up; absent when they are not signed up or anonymous")
        EventRegistrationStatus registrationStatus) {
    public static EventListItemResponse from(Event e, long seatsTaken, EventRegistrationStatus registrationStatus) {
        return new EventListItemResponse(
                e.getId(),
                e.getTitle(),
                e.getStartsAt(),
                e.getType(),
                e.getLocation(),
                e.getCoverImageUrl(),
                e.getSeatLimit(),
                (int) seatsTaken,
                registrationStatus);
    }
}
