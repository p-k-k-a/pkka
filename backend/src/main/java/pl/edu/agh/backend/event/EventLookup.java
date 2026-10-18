package pl.edu.agh.backend.event;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;

/**
 * Loads a single event on behalf of a caller. Kept apart from {@link EventService} so that registrations can
 * look events up while {@link EventService} reads registrations, without the two depending on each other.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventLookup {

    private final EventRepository eventRepository;

    public Event findVisible(UUID id, Caller caller) {
        return requireVisible(eventRepository.findById(id), () -> new EventNotFoundException(id), caller);
    }

    public Event findVisibleBySlug(String slug, Caller caller) {
        return requireVisible(eventRepository.findBySlug(slug), () -> new EventNotFoundException(slug), caller);
    }

    /** {@code MANDATORY} because a lock taken in a transaction of its own would be released too early. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Event findVisibleForUpdate(UUID id, Caller caller) {
        return requireVisible(eventRepository.findForUpdateById(id), () -> new EventNotFoundException(id), caller);
    }

    /** Not visible is reported as not existing, so the caller cannot probe for hidden events. */
    private Event requireVisible(Optional<Event> found, Supplier<EventNotFoundException> notFound, Caller caller) {
        Event event = found.orElseThrow(notFound);
        if (!EventVisibility.isVisibleTo(event, caller)) {
            throw notFound.get();
        }
        return event;
    }
}
