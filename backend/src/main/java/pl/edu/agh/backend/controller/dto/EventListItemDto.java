// controller/dto/EventListItemDto.java
package pl.edu.agh.backend.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import pl.edu.agh.backend.domain.Event;
import pl.edu.agh.backend.domain.EventType;

public record EventListItemDto(
        UUID id,
        String title,
        LocalDateTime startsAt,
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
