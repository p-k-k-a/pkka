package pl.edu.agh.backend.event.registration;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventLookup;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.EventSeatCount;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.OwnRegistration;
import pl.edu.agh.backend.event.registration.dto.EventRegistrationResponse;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventLookup eventLookup;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final CallerUserService callerUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Counting seats and inserting is a check-then-act, so it opens by locking the event row and the
     * unique constraint on {@code (event_id, user_id)} backs up the duplicate check below.
     */
    @Transactional
    public EventRegistrationResponse register(UUID eventId, Caller caller) {
        User user = callerUserService.getOrCreate(caller);
        Event event = eventLookup.findVisibleForUpdate(eventId, caller);

        if (isRegistrationClosed(event)) {
            throw EventRegistrationConflictException.registrationClosed(eventId);
        }
        if (eventRegistrationRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw EventRegistrationConflictException.alreadyRegistered(eventId);
        }

        long seatsTaken = seatsTaken(eventId);
        boolean takesASeat = hasFreeSeat(event, seatsTaken);

        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(user)
                .status(takesASeat ? EventRegistrationStatus.REGISTERED : EventRegistrationStatus.WAITLISTED)
                .build();
        try {
            eventRegistrationRepository.saveAndFlush(registration);
        } catch (DataIntegrityViolationException ex) {
            throw EventRegistrationConflictException.alreadyRegistered(eventId);
        }

        return toResponse(registration, event, seatsTaken + (takesASeat ? 1 : 0));
    }

    @Transactional(readOnly = true)
    public EventRegistrationResponse getOwnRegistration(UUID eventId, Caller caller) {
        Event event = eventLookup.findVisible(eventId, caller);
        EventRegistration registration = callerUserService
                .findId(caller)
                .flatMap(userId -> eventRegistrationRepository.findByEventIdAndUserId(eventId, userId))
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));

        long seatsTaken = seatsTaken(eventId);
        return toResponse(registration, event, seatsTaken);
    }

    @Transactional
    public void unregister(UUID eventId, Caller caller) {
        User user = callerUserService.getOrCreate(caller);
        Event event = eventLookup.findVisibleForUpdate(eventId, caller);
        if (hasStarted(event)) {
            throw EventRegistrationConflictException.eventAlreadyStarted(eventId);
        }

        EventRegistration registration = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));
        boolean freedASeat = registration.getStatus() == EventRegistrationStatus.REGISTERED;
        eventRegistrationRepository.delete(registration);

        if (freedASeat) {
            fillFreeSeats(event);
        }
    }

    /**
     * Moves as many people from the head of the waitlist onto a seat as the event has free seats — one after
     * a cancellation, possibly several after the seat limit is raised, everyone once the limit is lifted.
     * Nobody is promoted once the event has started, since the seat is no use to them any more.
     *
     * <p>{@code MANDATORY} because the caller must already hold the lock on the event row: two promotions
     * running side by side would read the same head of the queue.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void fillFreeSeats(Event event) {
        if (hasStarted(event)) {
            return;
        }
        eventRegistrationRepository.flush();
        Limit freeSeats = event.getSeatLimit() == null
                ? Limit.unlimited()
                : Limit.of((int) Math.max(0, event.getSeatLimit() - seatsTaken(event.getId())));
        if (freeSeats.isLimited() && freeSeats.max() == 0) {
            return;
        }
        eventRegistrationRepository
                .findByEventIdAndStatusOrderByRegisteredAtAscIdAsc(
                        event.getId(), EventRegistrationStatus.WAITLISTED, freeSeats)
                .forEach(next -> {
                    next.setStatus(EventRegistrationStatus.REGISTERED);
                    eventPublisher.publishEvent(new RegistrationPromotedEvent(
                            event.getId(), next.getUser().getId()));
                });
    }

    /** Only held seats count; the waitlist never inflates this number. */
    @Transactional(readOnly = true)
    public long seatsTaken(UUID eventId) {
        return eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
    }

    /** An event nobody holds a seat at is missing from the map rather than mapped to zero. */
    @Transactional(readOnly = true)
    public Map<UUID, Long> seatsTakenByEvent(Collection<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return eventRegistrationRepository
                .countByEventIdInAndStatus(eventIds, EventRegistrationStatus.REGISTERED)
                .stream()
                .collect(Collectors.toMap(EventSeatCount::eventId, EventSeatCount::seatsTaken));
    }

    @Transactional(readOnly = true)
    public Optional<EventRegistrationStatus> findOwnStatus(UUID eventId, Caller caller) {
        return callerUserService
                .findId(caller)
                .flatMap(userId -> eventRegistrationRepository.findStatus(eventId, userId));
    }

    /** Anonymous callers have no registrations, so they cost no query here. */
    @Transactional(readOnly = true)
    public Map<UUID, EventRegistrationStatus> ownStatusByEvent(Caller caller, Collection<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return callerUserService
                .findId(caller)
                .map(userId -> eventRegistrationRepository.findOwnRegistrations(userId, eventIds).stream()
                        .collect(Collectors.toMap(OwnRegistration::eventId, OwnRegistration::status)))
                .orElseGet(Map::of);
    }

    private EventRegistrationResponse toResponse(EventRegistration registration, Event event, long seatsTaken) {
        return new EventRegistrationResponse(
                event.getId(),
                registration.getRegisteredAt(),
                registration.getStatus(),
                waitlistPosition(registration),
                (int) seatsTaken,
                event.getSeatLimit());
    }

    private Integer waitlistPosition(EventRegistration registration) {
        if (registration.getStatus() != EventRegistrationStatus.WAITLISTED) {
            return null;
        }
        return (int) eventRegistrationRepository.countQueuedAhead(
                        registration.getEvent().getId(),
                        EventRegistrationStatus.WAITLISTED,
                        registration.getRegisteredAt(),
                        registration.getId())
                + 1;
    }

    private boolean hasFreeSeat(Event event, long seatsTaken) {
        return event.getSeatLimit() == null || seatsTaken < event.getSeatLimit();
    }

    private boolean isRegistrationClosed(Event event) {
        Instant closesAt = event.getRegistrationClosesAt();
        return hasStarted(event) || (closesAt != null && !Instant.now(clock).isBefore(closesAt));
    }

    private boolean hasStarted(Event event) {
        return !Instant.now(clock).isBefore(event.getStartsAt());
    }
}
