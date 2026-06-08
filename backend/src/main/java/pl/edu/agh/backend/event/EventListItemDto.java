package pl.edu.agh.backend.event;

import java.time.Instant;
import java.util.UUID;

public record EventListItemDto(
        UUID id,
        String title,
        Instant startsAt,
        EventType type,
        String location,
        String coverImageUrl,
        Integer seatLimit,
        Integer seatsTaken) {
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
