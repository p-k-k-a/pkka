package pl.edu.agh.backend.event.registration;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventService;
import pl.edu.agh.backend.event.registration.dto.EventRegistrationResponse;
import pl.edu.agh.backend.user.CurrentUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class EventRegistrationService {

    private final EventService eventService;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    /**
     * Counting seats and inserting is a check-then-act, so it runs in one transaction that starts by
     * locking the event row: competing sign-ups queue up there and each counts what the previous one
     * already committed. The unique constraint on {@code (event_id, user_id)} is the database-level
     * backstop for the duplicate check below.
     */
    @Transactional
    public EventRegistrationResponse register(UUID eventId, Authentication authentication) {
        User user = currentUserService.require(authentication);
        Event event = eventService.findVisibleForUpdate(eventId, authentication);

        if (isRegistrationClosed(event)) {
            throw EventRegistrationConflictException.registrationClosed(eventId);
        }
        if (eventRegistrationRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw EventRegistrationConflictException.alreadyRegistered(eventId);
        }

        long seatsTaken = eventRegistrationRepository.countByEventId(eventId);
        if (event.getSeatLimit() != null && seatsTaken >= event.getSeatLimit()) {
            throw EventRegistrationConflictException.noSeatsLeft(eventId);
        }

        EventRegistration registration =
                EventRegistration.builder().event(event).user(user).build();
        try {
            eventRegistrationRepository.saveAndFlush(registration);
        } catch (DataIntegrityViolationException ex) {
            throw EventRegistrationConflictException.alreadyRegistered(eventId);
        }

        return new EventRegistrationResponse(
                eventId, registration.getRegisteredAt(), (int) seatsTaken + 1, event.getSeatLimit());
    }

    /**
     * No lock needed: a delete only lowers the seat count. Allowed even after registration has closed —
     * keeping someone in a seat they no longer want helps nobody.
     */
    @Transactional
    public void unregister(UUID eventId, Authentication authentication) {
        User user = currentUserService.require(authentication);
        Event event = eventService.findVisible(eventId, authentication);

        EventRegistration registration = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));
        eventRegistrationRepository.delete(registration);
    }

    /** Closed at the organizer's deadline if there is one, and at the event's start in any case. */
    private boolean isRegistrationClosed(Event event) {
        Instant now = Instant.now(clock);
        Instant closesAt = event.getRegistrationClosesAt();
        return !now.isBefore(event.getStartsAt()) || (closesAt != null && !now.isBefore(closesAt));
    }
}
