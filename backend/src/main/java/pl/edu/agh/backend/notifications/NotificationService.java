package pl.edu.agh.backend.notifications;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter STARTS_AT = DateTimeFormatter.ofPattern("dd.MM 'o' HH:mm");

    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushClient expoPushClient;

    @Transactional
    public void announce(Event event) {
        List<DeviceToken> devices =
                switch (event.getAudience()) {
                    case PUBLIC -> deviceTokenRepository.findAll();
                    case ALL_ALUMNI -> deviceTokenRepository.findAllForApprovedAlumni();
                    case SPECIFIC_GROUP -> List.of();
                };
        push(devices, NotificationType.EVENT_ANNOUNCEMENT, "Nowe wydarzenie", event.getTitle(), event.getId());
    }

    @Transactional
    public void remind(Event event, Collection<UUID> userIds) {
        String startsAt = STARTS_AT.format(event.getStartsAt().atZone(POLAND));
        push(
                deviceTokenRepository.findByUserIdIn(userIds),
                NotificationType.EVENT_REMINDER,
                "Przypomnienie",
                "%s — %s".formatted(event.getTitle(), startsAt),
                event.getId());
    }

    private void push(List<DeviceToken> devices, NotificationType type, String title, String body, UUID targetId) {
        if (devices.isEmpty()) {
            return;
        }
        List<ExpoPushMessage> messages = devices.stream()
                .map(device -> new ExpoPushMessage(
                        device.getToken(),
                        title,
                        body,
                        type.getChannelId(),
                        Map.of("type", type.name(), "targetId", targetId.toString())))
                .toList();

        List<String> unreachable = expoPushClient.send(messages);
        if (!unreachable.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(unreachable);
        }
    }
}
