package pl.edu.agh.backend.event.registration;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventRegistrationConflictException extends RuntimeException {

    public enum Reason {
        ALREADY_REGISTERED,
        REGISTRATION_CLOSED,
        NO_SEATS_LEFT
    }

    private final Reason reason;

    private EventRegistrationConflictException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public static EventRegistrationConflictException alreadyRegistered(UUID eventId) {
        return new EventRegistrationConflictException(
                Reason.ALREADY_REGISTERED, "Already registered for event %s".formatted(eventId));
    }

    public static EventRegistrationConflictException registrationClosed(UUID eventId) {
        return new EventRegistrationConflictException(
                Reason.REGISTRATION_CLOSED, "Registration for event %s is closed".formatted(eventId));
    }

    public static EventRegistrationConflictException noSeatsLeft(UUID eventId) {
        return new EventRegistrationConflictException(
                Reason.NO_SEATS_LEFT, "Event %s has no seats left".formatted(eventId));
    }
}
