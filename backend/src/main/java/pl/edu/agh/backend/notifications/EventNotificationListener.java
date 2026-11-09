package pl.edu.agh.backend.notifications;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.event.EventCancelledEvent;
import pl.edu.agh.backend.event.EventCreatedEvent;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventScheduleChangedEvent;
import pl.edu.agh.backend.event.message.EventMessageSentEvent;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.RegistrationPromotedEvent;

@Component
@RequiredArgsConstructor
public class EventNotificationListener {

    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final EventMailService eventMailService;
    private final EventRegistrationRepository eventRegistrationRepository;

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

    // Changes, cancellations and admin messages reach everyone signed up, waitlist included. Push and mail are
    // separate listeners, as for a promotion: a failed push does not cost the mail, and only the push, which
    // records its tickets, runs in a transaction.

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScheduleChanged(EventScheduleChangedEvent changed) {
        eventRepository
                .findById(changed.eventId())
                .ifPresent(event -> notificationService.announceChange(event, changed, signedUp(event.getId())));
    }

    @Async
    @TransactionalEventListener
    public void mailScheduleChange(EventScheduleChangedEvent changed) {
        eventRepository
                .findById(changed.eventId())
                .ifPresent(event -> eventMailService.mailScheduleChange(event, changed, signedUp(event.getId())));
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCancelled(EventCancelledEvent cancelled) {
        notificationService.announceCancellation(cancelled, signedUp(cancelled.eventId()));
    }

    @Async
    @TransactionalEventListener
    public void mailCancellation(EventCancelledEvent cancelled) {
        eventMailService.mailCancellation(cancelled, signedUp(cancelled.eventId()));
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageSent(EventMessageSentEvent message) {
        eventRepository
                .findById(message.eventId())
                .ifPresent(event ->
                        notificationService.announceMessage(event, message.subject(), signedUp(event.getId())));
    }

    @Async
    @TransactionalEventListener
    public void mailMessage(EventMessageSentEvent message) {
        eventRepository
                .findById(message.eventId())
                .ifPresent(event -> eventMailService.mailMessage(
                        event, message.subject(), message.body(), signedUp(event.getId())));
    }

    private List<UUID> signedUp(UUID eventId) {
        return eventRegistrationRepository.findRecipientIds(eventId);
    }
}
