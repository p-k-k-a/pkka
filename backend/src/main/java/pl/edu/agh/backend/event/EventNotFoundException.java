package pl.edu.agh.backend.event;

import java.util.UUID;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(UUID id) {
        super("Event with id %s not found".formatted(id));
    }
}
