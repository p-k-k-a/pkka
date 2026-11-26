package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "${app.notifications.reminder-cron:0 * * * * *}")
    @Transactional
    public void sendDueReminders() {
        List<EventRegistration> due = eventRegistrationRepository.findDueForReminder();
        Instant sentAt = Instant.now();
        Map<UUID, List<EventRegistration>> byEvent = due.stream()
                .collect(Collectors.groupingBy(
                        registration -> registration.getEvent().getId()));

        byEvent.values().forEach(group -> {
            List<UUID> userIds = group.stream()
                    .map(registration -> registration.getUser().getId())
                    .toList();
            Set<UUID> unreached =
                    notificationService.pushReminder(group.getFirst().getEvent(), userIds);
            List<EventRegistration> reminded = group.stream()
                    .filter(registration ->
                            !unreached.contains(registration.getUser().getId()))
                    .toList();
            reminded.forEach(registration -> registration.setReminderSentAt(sentAt));
            // Only the people marked as reminded, so a retried push does not mail them twice.
            eventPublisher.publishEvent(new RemindersSentEvent(
                    group.getFirst().getEvent().getId(),
                    reminded.stream()
                            .map(registration -> registration.getUser().getId())
                            .toList()));
        });
    }
}
