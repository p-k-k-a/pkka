package pl.edu.agh.backend.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.event.EventCreatedEvent;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.RegistrationPromotedEvent;

@Component
@RequiredArgsConstructor
public class EventNotificationListener {

    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final EventMailService eventMailService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onEventCreated(EventCreatedEvent event) {
        eventRepository.findById(event.eventId()).ifPresent(notificationService::announce);
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRegistrationPromoted(RegistrationPromotedEvent promoted) {
        eventRepository
                .findById(promoted.eventId())
                .ifPresent(event -> notificationService.announcePromotion(event, promoted.userId()));
    }

    /**
     * Apart from the push, so a failed push does not cost the mail. No transaction around it: the event and
     * the address are read up front and the SMTP call should not hold a database connection.
     */
    @Async
    @TransactionalEventListener
    public void mailPromotion(RegistrationPromotedEvent promoted) {
        eventRepository
                .findById(promoted.eventId())
                .ifPresent(event -> eventMailService.mailPromotion(event, promoted.userId()));
    }
}
