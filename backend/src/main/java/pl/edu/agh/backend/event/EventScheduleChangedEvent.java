package pl.edu.agh.backend.event;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * An admin moved an event or changed where it happens. Carries the values from before the edit, so the people
 * signed up can be told what it was and what it is now.
 *
 * <p>Times are compared to the minute and text ignoring surrounding spaces, with blank meaning not given, so
 * an edit people would not see as a change (seconds, a trailing space) tells nobody.
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
        return !sameMinute(previousStartsAt, current.getStartsAt()) || !sameMinute(previousEndsAt, current.getEndsAt());
    }

    public boolean typeChanged(Event current) {
        return previousType != current.getType();
    }

    public boolean locationChanged(Event current) {
        return !sameText(previousLocation, current.getLocation());
    }

    public boolean transmissionUrlChanged(Event current) {
        return !sameText(previousTransmissionUrl, current.getTransmissionUrl());
    }

    public boolean anyChange(Event current) {
        return timeChanged(current)
                || typeChanged(current)
                || locationChanged(current)
                || transmissionUrlChanged(current);
    }

    private static boolean sameMinute(Instant previous, Instant current) {
        return Objects.equals(toMinute(previous), toMinute(current));
    }

    private static Instant toMinute(Instant instant) {
        return instant == null ? null : instant.truncatedTo(ChronoUnit.MINUTES);
    }

    private static boolean sameText(String previous, String current) {
        return Objects.equals(normalize(previous), normalize(current));
    }

    private static String normalize(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
