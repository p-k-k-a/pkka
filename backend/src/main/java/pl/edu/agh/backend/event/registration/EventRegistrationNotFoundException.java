package pl.edu.agh.backend.event.registration;

import java.util.UUID;

public class EventRegistrationNotFoundException extends RuntimeException {
    public EventRegistrationNotFoundException(UUID eventId) {
        super("The current user is not registered for event %s".formatted(eventId));
    }
}
