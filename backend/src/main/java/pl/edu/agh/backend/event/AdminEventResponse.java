package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import pl.edu.agh.backend.event.tag.Tag;


public record AdminEventResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String shortDescription,
        String fullDescription,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        String transmissionUrl,
        String location,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Registrations for this event so far")
        int seatsTaken,

        Instant registrationClosesAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Audience audience,
        String coverImageUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) Set<String> tags,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminEventResponse from(Event event, long seatsTaken) {
        return new AdminEventResponse(
                event.getId(),
                event.getTitle(),
                event.getShortDescription(),
                event.getFullDescription(),
                event.getType(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getTransmissionUrl(),
                event.getLocation(),
                event.getSeatLimit(),
                (int) seatsTaken,
                event.getRegistrationClosesAt(),
                event.getAudience(),
                event.getCoverImageUrl(),
                event.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
