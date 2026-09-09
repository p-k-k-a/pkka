package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;

@Component
@RequiredArgsConstructor
public class EventReminderScheduler {

    private final EventRegistrationRepository eventRegistrationRepository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;

    /**
     * Sends everything already due rather than everything inside a one-hour window, so a sweep the server missed
     * catches up on the next run instead of dropping those reminders for good.
     */
    @Scheduled(cron = "${app.notifications.reminder-cron:0 0 * * * *}")
    @Transactional
    public void sendDueReminders() {
        if (!properties.enabled()) {
            return;
        }

        List<EventRegistration> due = eventRegistrationRepository.findDueForReminder();
        Instant sentAt = Instant.now();
        Map<UUID, List<EventRegistration>> byEvent = due.stream()
                .collect(Collectors.groupingBy(
                        registration -> registration.getEvent().getId()));

        byEvent.values().forEach(group -> {
            List<UUID> userIds = group.stream()
                    .map(registration -> registration.getUser().getId())
                    .toList();
            notificationService.remind(group.getFirst().getEvent(), userIds);
            group.forEach(registration -> registration.setReminderSentAt(sentAt));
        });
    }
}
