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
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        String location,

        @Schema(description = "Signed, short-lived URL when the image is an uploaded file, otherwise the external URL")
        String coverImageUrl,

        @Schema(description = "Absent when the event has no seat limit")
        Integer seatLimit,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Registrations for this event so far")
        int seatsTaken,

        @Schema(description = "The requesting user's own sign-up; absent when they are not signed up or anonymous")
        EventRegistrationStatus registrationStatus) {
    public static EventListItemResponse from(
            Event e, long seatsTaken, EventRegistrationStatus registrationStatus, String coverImageUrl) {
        return new EventListItemResponse(
                e.getId(),
                e.getSlug(),
                e.getTitle(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getType(),
                e.getLocation(),
                coverImageUrl,
                e.getSeatLimit(),
                (int) seatsTaken,
                registrationStatus);
    }
}
