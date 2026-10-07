package pl.edu.agh.backend.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.event.tag.Tag;

public record EventDetailsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String fullDescription,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        String location,

        @Schema(description = "Absent when the event has no seat limit")
        Integer seatLimit,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Registrations for this event so far")
        int seatsTaken,

        Instant registrationClosesAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Audience audience,
        String coverImageUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) Set<String> tags,

        @Schema(description = "The requesting user's own sign-up; absent when they are not signed up or anonymous")
        EventRegistrationStatus registrationStatus) {
    public static EventDetailsResponse from(Event e, long seatsTaken, EventRegistrationStatus registrationStatus) {
        return new EventDetailsResponse(
                e.getId(),
                e.getTitle(),
                e.getFullDescription(),
                e.getType(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getLocation(),
                e.getSeatLimit(),
                (int) seatsTaken,
                e.getRegistrationClosesAt(),
                e.getAudience(),
                e.getCoverImageUrl(),
                e.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                registrationStatus);
    }
}
