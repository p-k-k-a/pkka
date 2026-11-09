package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventCancelledEvent;
import pl.edu.agh.backend.event.EventScheduleChangedEvent;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** Mails about an event, next to the pushes {@link NotificationService} sends for the same moments. */
@Service
@RequiredArgsConstructor
public class EventMailService {

    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMMM yyyy", POLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", POLISH);
    private static final DateTimeFormatter STARTS_AT = DateTimeFormatter.ofPattern("d MMMM yyyy 'o' HH:mm", POLISH);
    private static final String NOT_GIVEN = "nie podano";

    private final MailService mailService;
    private final UserRepository userRepository;

    /**
     * Sent whatever the user's devices or push settings: a promotion can come just before the event, and a
     * push never reaches someone who only uses the web.
     */
    public void mailPromotion(Event event, UUID userId) {
        mailEach(
                List.of(userId),
                "Zwolniło się miejsce: " + event.getTitle(),
                "registration-promoted",
                eventVariables(event));
    }

    public void mailScheduleChange(Event event, EventScheduleChangedEvent before, Collection<UUID> userIds) {
        Map<String, Object> variables = eventVariables(event);
        variables.put("changes", describeChanges(event, before));
        mailEach(userIds, "Zmiana w wydarzeniu: " + event.getTitle(), "event-changed", variables);
    }

    public void mailCancellation(EventCancelledEvent cancelled, Collection<UUID> userIds) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", cancelled.title());
        variables.put("startsAt", STARTS_AT.format(cancelled.startsAt().atZone(POLAND)));
        mailEach(userIds, "Wydarzenie odwołane: " + cancelled.title(), "event-cancelled", variables);
    }

    public void mailMessage(Event event, String subject, String body, Collection<UUID> userIds) {
        Map<String, Object> variables = eventVariables(event);
        variables.put("message", body);
        mailEach(userIds, subject, "event-message", variables);
    }

    /** One mail per person, so nobody sees who else is signed up. */
    private void mailEach(Collection<UUID> userIds, String subject, String template, Map<String, Object> variables) {
        if (userIds.isEmpty()) {
            return;
        }
        userRepository.findAllById(userIds).stream()
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .forEach(email -> mailService.send(email, subject, template, variables));
    }

    private static Map<String, Object> eventVariables(Event event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", event.getTitle());
        variables.put("slug", event.getSlug());
        variables.put("startsAt", STARTS_AT.format(event.getStartsAt().atZone(POLAND)));
        variables.put("location", event.getLocation());
        return variables;
    }

    static List<String> describeChanges(Event event, EventScheduleChangedEvent before) {
        List<String> changes = new ArrayList<>();
        if (before.timeChanged(event)) {
            changes.add("Termin: %s → %s"
                    .formatted(
                            timeRange(before.previousStartsAt(), before.previousEndsAt()),
                            timeRange(event.getStartsAt(), event.getEndsAt())));
        }
        if (before.typeChanged(event)) {
            changes.add("Forma: %s → %s".formatted(label(before.previousType()), label(event.getType())));
        }
        if (before.locationChanged(event)) {
            changes.add("Miejsce: %s → %s"
                    .formatted(orNotGiven(before.previousLocation()), orNotGiven(event.getLocation())));
        }
        if (before.transmissionUrlChanged(event)) {
            changes.add("Zmienił się link do transmisji, aktualny jest na stronie wydarzenia");
        }
        return changes;
    }

    static String timeRange(Instant startsAt, Instant endsAt) {
        ZonedDateTime start = startsAt.atZone(POLAND);
        if (endsAt == null) {
            return DAY.format(start) + ", " + TIME.format(start);
        }
        ZonedDateTime end = endsAt.atZone(POLAND);
        if (start.toLocalDate().equals(end.toLocalDate())) {
            return "%s, %s–%s".formatted(DAY.format(start), TIME.format(start), TIME.format(end));
        }
        return "%s, %s – %s, %s".formatted(DAY.format(start), TIME.format(start), DAY.format(end), TIME.format(end));
    }

    private static String label(EventType type) {
        if (type == null) {
            return NOT_GIVEN;
        }
        return switch (type) {
            case ONLINE -> "online";
            case IN_PERSON -> "stacjonarnie";
            case HYBRID -> "hybrydowo";
        };
    }

    private static String orNotGiven(String value) {
        return StringUtils.hasText(value) ? value : NOT_GIVEN;
    }
}
