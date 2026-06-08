package pl.edu.agh.backend.event;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record EventDetailsDto(
        UUID id,
        String title,
        String fullDescription,
        EventType type,
        Instant startsAt,
        Instant endsAt,
        String location,
        Integer seatLimit,
        Integer seatsTaken,
        Instant registrationClosesAt,
        Audience audience,
        String coverImageUrl,
        Set<String> tags) {
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
