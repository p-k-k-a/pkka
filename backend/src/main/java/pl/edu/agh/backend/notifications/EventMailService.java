package pl.edu.agh.backend.notifications;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** Mails about an event, next to the pushes {@link NotificationService} sends for the same moments. */
@Service
@RequiredArgsConstructor
public class EventMailService {

    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter STARTS_AT =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'o' HH:mm", Locale.forLanguageTag("pl-PL"));

    private final MailService mailService;
    private final UserRepository userRepository;

    /**
     * Sent whatever the user's devices or push settings: a promotion can come just before the event, and a
     * push never reaches someone who only uses the web.
     */
    public void mailPromotion(Event event, UUID userId) {
        userRepository
                .findById(userId)
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .ifPresent(email -> mailService.send(
                        email,
                        "Zwolniło się miejsce: " + event.getTitle(),
                        "registration-promoted",
                        eventVariables(event)));
    }

    private static Map<String, Object> eventVariables(Event event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", event.getTitle());
        variables.put("slug", event.getSlug());
        variables.put("startsAt", STARTS_AT.format(event.getStartsAt().atZone(POLAND)));
        variables.put("location", event.getLocation());
        return variables;
    }
}
