package pl.edu.agh.backend.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * An admin moved an event or changed where it happens. Carries the values from before the edit, so the people
 * signed up can be told what it was and what it is now.
 */
public record EventScheduleChangedEvent(
        UUID eventId,
        Instant previousStartsAt,
        Instant previousEndsAt,
        EventType previousType,
        String previousLocation,
        String previousTransmissionUrl) {

    static EventScheduleChangedEvent before(Event event) {
        return new EventScheduleChangedEvent(
                event.getId(),
                event.getStartsAt(),
                event.getEndsAt(),
                event.getType(),
                event.getLocation(),
                event.getTransmissionUrl());
    }

    public boolean timeChanged(Event current) {
        return !Objects.equals(previousStartsAt, current.getStartsAt())
                || !Objects.equals(previousEndsAt, current.getEndsAt());
    }

    public boolean typeChanged(Event current) {
        return previousType != current.getType();
    }

    public boolean locationChanged(Event current) {
        return !Objects.equals(previousLocation, current.getLocation());
    }

    public boolean transmissionUrlChanged(Event current) {
        return !Objects.equals(previousTransmissionUrl, current.getTransmissionUrl());
    }

    public boolean anyChange(Event current) {
        return timeChanged(current)
                || typeChanged(current)
                || locationChanged(current)
                || transmissionUrlChanged(current);
    }
}
