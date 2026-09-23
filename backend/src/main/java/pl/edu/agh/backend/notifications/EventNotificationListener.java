package pl.edu.agh.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.event.EventCreatedEvent;
import pl.edu.agh.backend.event.EventRepository;

@Component
@RequiredArgsConstructor
public class EventNotificationListener {

    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    /**
     * After commit, so an event that failed to save never announces itself, and {@code @Async} so the admin's
     * request neither waits for Expo nor fails when Expo does. {@code REQUIRES_NEW} keeps the dead-token cleanup
     * in a transaction of its own rather than one that is already completing.
     */
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCreated(EventCreatedEvent event) {
        eventRepository.findById(event.eventId()).ifPresent(notificationService::announce);
    }
}
