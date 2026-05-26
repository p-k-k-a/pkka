// controller/dto/EventDetailsDto.java
package pl.edu.agh.backend.controller.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import pl.edu.agh.backend.domain.Audience;
import pl.edu.agh.backend.domain.Event;
import pl.edu.agh.backend.domain.EventType;
import pl.edu.agh.backend.domain.Tag;

public record EventDetailsDto(
        UUID id,
        String title,
        String fullDescription,
        EventType type,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location,
        Integer seatLimit,
        Integer seatsTaken,
        boolean waitingListEnabled,
        LocalDateTime registrationClosesAt,
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
                e.isWaitingListEnabled(),
                e.getRegistrationClosesAt(),
                e.getAudience(),
                e.getCoverImageUrl(),
                e.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
    }
}
