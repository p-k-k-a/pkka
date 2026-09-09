package pl.edu.agh.backend.notifications;

import lombok.RequiredArgsConstructor;
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
    private final NotificationProperties properties;

    /**
     * After commit, so an event that failed to save never announces itself. {@code REQUIRES_NEW} because the
     * original transaction is already completing here: a write that joined it would be dropped without error.
     */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCreated(EventCreatedEvent event) {
        if (!properties.enabled()) {
            return;
        }
        eventRepository.findById(event.eventId()).ifPresent(notificationService::announce);
    }
}
