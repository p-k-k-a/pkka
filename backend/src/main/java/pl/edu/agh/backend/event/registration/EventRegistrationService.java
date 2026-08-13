package pl.edu.agh.backend.event.registration;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    /** No lock needed: a delete only lowers the seat count. Allowed after registration has closed. */
    @Transactional
    public void unregister(UUID eventId, Caller caller) {
        User user = callerUserService.getOrCreate(caller);
        Event event = eventService.findVisible(eventId, caller);

        EventRegistration registration = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), user.getId())
                .orElseThrow(() -> new EventRegistrationNotFoundException(eventId));
        eventRegistrationRepository.delete(registration);
    }

    private boolean isRegistrationClosed(Event event) {
        Instant now = Instant.now(clock);
        Instant closesAt = event.getRegistrationClosesAt();
        return !now.isBefore(event.getStartsAt()) || (closesAt != null && !now.isBefore(closesAt));
    }
}
