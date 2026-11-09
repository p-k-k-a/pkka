package pl.edu.agh.backend.event.message;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;

@Service
@RequiredArgsConstructor
public class AdminEventMessageService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public EventMessageRecipientsResponse recipients(UUID eventId) {
        requireEvent(eventId);
        return new EventMessageRecipientsResponse(eventRegistrationRepository.countRecipients(eventId));
    }

    /** The message itself goes out after commit and off the request thread; this only queues it. */
    @Transactional
    public EventMessageRecipientsResponse send(UUID eventId, EventMessageRequest request) {
        requireEvent(eventId);
        long recipients = eventRegistrationRepository.countRecipients(eventId);
        if (recipients > 0) {
            eventPublisher.publishEvent(new EventMessageSentEvent(
                    eventId, request.subject().strip(), request.body().strip()));
        }
        return new EventMessageRecipientsResponse(recipients);
    }

    private void requireEvent(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
    }
}
