package pl.edu.agh.backend.event.registration;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventService;
import pl.edu.agh.backend.event.registration.dto.EventRegistrationResponse;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventService eventService;
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
        Event event = eventService.findVisibleForUpdate(eventId, caller);

        if (isRegistrationClosed(event)) {
            throw EventRegistrationConflictException.registrationClosed(eventId);
        }
        if (eventRegistrationRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw EventRegistrationConflictException.alreadyRegistered(eventId);
        }

        long seatsTaken =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
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
        Event event = eventService.findVisible(eventId, caller);
        EventRegistration registration = callerUserService
                .findId(caller)
                .flatMap(userId -> eventRegistrationRepository.findByEventIdAndUserId(eventId, userId))
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));

        long seatsTaken =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
        return toResponse(registration, event, seatsTaken);
    }

    @Transactional
    public void unregister(UUID eventId, Caller caller) {
        User user = callerUserService.getOrCreate(caller);
        Event event = eventService.findVisibleForUpdate(eventId, caller);
        if (hasStarted(event)) {
            throw EventRegistrationConflictException.eventAlreadyStarted(eventId);
        }

        EventRegistration registration = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));
        boolean freedASeat = registration.getStatus() == EventRegistrationStatus.REGISTERED;
        eventRegistrationRepository.delete(registration);

        if (freedASeat) {
            promoteHeadOfQueue(event);
        }
    }

    private void promoteHeadOfQueue(Event event) {
        eventRegistrationRepository
                .findFirstByEventIdAndStatusOrderByRegisteredAtAscIdAsc(
                        event.getId(), EventRegistrationStatus.WAITLISTED)
                .ifPresent(next -> {
                    next.setStatus(EventRegistrationStatus.REGISTERED);
                    eventPublisher.publishEvent(new RegistrationPromotedEvent(
                            event.getId(), next.getUser().getId()));
                });
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
