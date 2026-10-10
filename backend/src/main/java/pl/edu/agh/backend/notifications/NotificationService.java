package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
import pl.edu.agh.backend.notifications.expo.ExpoPushMessage;
import pl.edu.agh.backend.notifications.expo.SendOutcome;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter STARTS_AT = DateTimeFormatter.ofPattern("dd.MM 'o' HH:mm");

    private final DeviceTokenRepository deviceTokenRepository;
    private final ExpoPushClient expoPushClient;
    private final PushTicketRepository pushTicketRepository;

    public void announce(Event event) {
        List<DeviceToken> devices =
                switch (event.getAudience()) {
                    case PUBLIC -> deviceTokenRepository.findAll();
                    case ALL_ALUMNI -> deviceTokenRepository.findAllForApprovedAlumni();
                    case SPECIFIC_GROUP -> List.of();
                };
        push(devices, NotificationType.EVENT_ANNOUNCEMENT, "Nowe wydarzenie", event.getTitle(), event.getId());
    }

    public Set<UUID> remind(Event event, Collection<UUID> userIds) {
        String startsAt = STARTS_AT.format(event.getStartsAt().atZone(POLAND));
        List<DeviceToken> devices = deviceTokenRepository.findByUserIdIn(userIds);
        Set<String> refusedTokens = push(
                devices,
                NotificationType.EVENT_REMINDER,
                "Przypomnienie",
                "%s - %s".formatted(event.getTitle(), startsAt),
                event.getId());
        Map<UUID, List<DeviceToken>> devicesByUser = devices.stream()
                .collect(Collectors.groupingBy(device -> device.getUser().getId()));
        return devicesByUser.entrySet().stream()
                .filter(entry ->
                        entry.getValue().stream().allMatch(device -> refusedTokens.contains(device.getToken())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> push(
            List<DeviceToken> devices, NotificationType type, String title, String body, UUID targetId) {
        if (devices.isEmpty()) {
            return Set.of();
        }
        List<ExpoPushMessage> messages = devices.stream()
                .map(device -> new ExpoPushMessage(
                        device.getToken(),
                        title,
                        body,
                        type.getChannelId(),
                        new NotificationPayload(type, targetId).toData()))
                .toList();

        SendOutcome outcome = expoPushClient.send(messages);
        Instant sentAt = Instant.now();
        pushTicketRepository.saveAll(outcome.tickets().entrySet().stream()
                .map(ticket -> PushTicket.builder()
                        .ticketId(ticket.getKey())
                        .device(deviceTokenRepository.getReferenceById(ticket.getValue()))
                        .createdAt(sentAt)
                        .build())
                .toList());
        return outcome.refusedTokens();
    }
}
