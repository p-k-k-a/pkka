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

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCreated(EventCreatedEvent event) {
        eventRepository.findById(event.eventId()).ifPresent(notificationService::announce);
    }
}
