package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.dto.EventDetailsResponse;
import pl.edu.agh.backend.event.dto.EventListItemResponse;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.EventSeatCount;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final CallerUserService callerUserService;

    public Page<EventListItemResponse> list(Caller caller, Collection<String> tagNames, Pageable pageable) {
        Specification<Event> spec = Specification.allOf(
                startsAfter(Instant.now()), audienceIn(EventVisibility.audiencesOf(caller)), hasAnyTag(tagNames));

        Page<Event> events = eventRepository.findAll(spec, pageable);
        List<UUID> ids = events.getContent().stream().map(Event::getId).toList();
        Map<UUID, Long> seatsTaken = seatsTakenByEvent(ids);
        Set<UUID> registered = registeredEventIds(caller, ids);

        return events.map(event -> EventListItemResponse.from(
                event, seatsTaken.getOrDefault(event.getId(), 0L), registered.contains(event.getId())));
    }

    public EventDetailsResponse getDetails(UUID id, Caller caller) {
        Event event = findVisible(id, caller);
        boolean registered = currentUserId(caller)
                .map(userId -> eventRegistrationRepository.existsByEventIdAndUserId(id, userId))
                .orElse(false);
        return EventDetailsResponse.from(event, eventRegistrationRepository.countByEventId(id), registered);
    }

    public Event findVisible(UUID id, Caller caller) {
        return requireVisible(eventRepository.findById(id), id, caller);
    }

    /** {@code MANDATORY} because a lock taken in a transaction of its own would be released too early. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Event findVisibleForUpdate(UUID id, Caller caller) {
        return requireVisible(eventRepository.findForUpdateById(id), id, caller);
    }

    /** Not visible is reported as not existing, so the caller cannot probe for hidden events. */
    private Event requireVisible(Optional<Event> found, UUID id, Caller caller) {
        Event event = found.orElseThrow(() -> new EventNotFoundException(id));
        if (!EventVisibility.isVisibleTo(event, caller)) {
            throw new EventNotFoundException(id);
        }
        return event;
    }

    private Map<UUID, Long> seatsTakenByEvent(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return eventRegistrationRepository.countByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(EventSeatCount::getEventId, EventSeatCount::getSeatsTaken));
    }

    /** Anonymous callers have no registrations, so they cost no query here. */
    private Set<UUID> registeredEventIds(Caller caller, List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return Set.of();
        }
        return currentUserId(caller)
                .map(userId -> eventRegistrationRepository.findRegisteredEventIds(userId, eventIds))
                .orElseGet(Set::of);
    }

    private Optional<UUID> currentUserId(Caller caller) {
        return callerUserService.find(caller).map(User::getId);
    }
}
