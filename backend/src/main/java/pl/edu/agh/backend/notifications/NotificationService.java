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
import pl.edu.agh.backend.event.EventCancelledEvent;
import pl.edu.agh.backend.event.EventScheduleChangedEvent;
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
        push(devices, NotificationType.EVENT_ANNOUNCEMENT, "Nowe wydarzenie", event.getTitle(), event.getSlug());
    }

    public Set<UUID> remind(Event event, Collection<UUID> userIds) {
        String startsAt = STARTS_AT.format(event.getStartsAt().atZone(POLAND));
        List<DeviceToken> devices = deviceTokenRepository.findByUserIdIn(userIds);
        Set<String> refusedTokens = push(
                devices,
                NotificationType.EVENT_REMINDER,
                "Przypomnienie",
                "%s - %s".formatted(event.getTitle(), startsAt),
                event.getSlug());
        Map<UUID, List<DeviceToken>> devicesByUser = devices.stream()
                .collect(Collectors.groupingBy(device -> device.getUser().getId()));
        return devicesByUser.entrySet().stream()
                .filter(entry ->
                        entry.getValue().stream().allMatch(device -> refusedTokens.contains(device.getToken())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public void announcePromotion(Event event, UUID userId) {
        String startsAt = STARTS_AT.format(event.getStartsAt().atZone(POLAND));
        push(
                deviceTokenRepository.findByUserIdIn(List.of(userId)),
                NotificationType.REGISTRATION_PROMOTED,
                "Zwolniło się miejsce",
                "Zwolniło się miejsce i masz zapis na wydarzenie „%s”, %s".formatted(event.getTitle(), startsAt),
                event.getSlug());
    }

    public void announceChange(Event event, EventScheduleChangedEvent before, Collection<UUID> userIds) {
        String body = before.timeChanged(event)
                ? "„%s”: nowy termin %s"
                        .formatted(
                                event.getTitle(),
                                STARTS_AT.format(event.getStartsAt().atZone(POLAND)))
                : "„%s”: zmiana miejsca".formatted(event.getTitle());
        pushToSignedUp(userIds, NotificationType.EVENT_CHANGED, "Zmiana w wydarzeniu", body, event.getSlug());
    }

    public void announceCancellation(EventCancelledEvent cancelled, Collection<UUID> userIds) {
        pushToSignedUp(
                userIds,
                NotificationType.EVENT_CANCELLED,
                "Wydarzenie odwołane",
                "„%s” (%s) zostało odwołane"
                        .formatted(
                                cancelled.title(),
                                STARTS_AT.format(cancelled.startsAt().atZone(POLAND))),
                cancelled.slug());
    }

    public void announceMessage(Event event, String subject, Collection<UUID> userIds) {
        pushToSignedUp(userIds, NotificationType.EVENT_MESSAGE, event.getTitle(), subject, event.getSlug());
    }

    private void pushToSignedUp(
            Collection<UUID> userIds, NotificationType type, String title, String body, String eventSlug) {
        if (!userIds.isEmpty()) {
            push(deviceTokenRepository.findByUserIdIn(userIds), type, title, body, eventSlug);
        }
    }

    private Set<String> push(
            List<DeviceToken> devices, NotificationType type, String title, String body, String target) {
        if (devices.isEmpty()) {
            return Set.of();
        }
        List<ExpoPushMessage> messages = devices.stream()
                .map(device -> new ExpoPushMessage(
                        device.getToken(),
                        title,
                        body,
                        type.getChannelId(),
                        new NotificationPayload(type, target).toData()))
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
