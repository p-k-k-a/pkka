package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record EventDetailsDto(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String fullDescription,
        @Schema(requiredMode = RequiredMode.REQUIRED) EventType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        String location,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED) Integer seatsTaken,
        Instant registrationClosesAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Audience audience,
        String coverImageUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) Set<String> tags) {
    public static EventDetailsDto from(Event e) {
        return new EventDetailsDto(
                e.getId(),
                e.getTitle(),
                e.getFullDescription(),
                e.getType(),
                e.getStartsAt(),
                e.getEndsAt(),
                e.getLocation(),
                e.getSeatLimit(),
                0, // TODO(events-registrations)
                e.getRegistrationClosesAt(),
                e.getAudience(),
                e.getCoverImageUrl(),
                e.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
    }
}
