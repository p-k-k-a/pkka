package pl.edu.agh.backend.notifications;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.RegistrationPromotedEvent;

/**
 * Tells someone on a waitlist that they got a seat. Runs after the promotion commits, so a rolled-back
 * promotion never notifies anyone, and off the request thread, so a slow push service doesn't hold up the
 * cancellation or seat-limit change that freed the seat.
 */
@Component
@RequiredArgsConstructor
class RegistrationPromotedNotifier {

    static final String TYPE = "REGISTRATION_PROMOTED";

    private final DeviceTokenRepository deviceTokenRepository;
    private final EventRepository eventRepository;
    private final ExpoPushClient expoPushClient;

    @Async
    @TransactionalEventListener
    public void onRegistrationPromoted(RegistrationPromotedEvent promoted) {
        List<DeviceToken> devices = deviceTokenRepository.findAllByUserId(promoted.userId());
        if (devices.isEmpty()) {
            return;
        }
        String body = eventRepository
                .findById(promoted.eventId())
                .map(Event::getTitle)
                .map(title -> "Zwolniło się miejsce na wydarzeniu „%s” — jesteś zapisany/a.".formatted(title))
                .orElse("Zwolniło się miejsce na wydarzeniu — jesteś zapisany/a.");
        Map<String, String> data =
                Map.of("type", TYPE, "eventId", promoted.eventId().toString());
        expoPushClient.send(devices.stream()
                .map(device -> new PushMessage(device.getToken(), "Masz miejsce!", body, data))
                .toList());
    }
}
