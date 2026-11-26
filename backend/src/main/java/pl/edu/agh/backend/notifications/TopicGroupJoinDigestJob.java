package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.topicgroup.TopicGroupMembershipRepository;
import pl.edu.agh.backend.topicgroup.TopicGroupMembershipRepository.JoinDigest;
import pl.edu.agh.backend.topicgroup.TopicGroupRepository;

/** Once a day tells each coordinator how many people joined their group since the last digest. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TopicGroupJoinDigestJob {

    private final TopicGroupMembershipRepository membershipRepository;
    private final TopicGroupRepository topicGroupRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    /** Sends and records each digest in one transaction, so a restart neither repeats nor loses one. */
    @Scheduled(cron = "${app.notifications.group-digest-cron:0 0 18 * * *}", zone = "Europe/Warsaw")
    @Transactional
    public void sendDigests() {
        Instant until = Instant.now();
        for (JoinDigest digest : membershipRepository.findJoinDigests(until)) {
            topicGroupRepository.setLastJoinDigestAt(digest.getGroupId(), until);
            String newMembers = newMembers(digest.getNewMembers());
            String title = "%s w grupie „%s”".formatted(newMembers, digest.getGroupName());
            try {
                notificationService.pushToUser(
                        digest.getCoordinatorId(),
                        NotificationType.TOPIC_GROUP_NEW_MEMBERS,
                        title,
                        "Zobacz, kto dołączył do grupy, którą koordynujesz.",
                        digest.getGroupSlug());
            } catch (RuntimeException ex) {
                log.warn("Could not push the join digest of group {}: {}", digest.getGroupSlug(), ex.toString());
            }
            if (StringUtils.hasText(digest.getCoordinatorEmail())) {
                String joined = plural(digest.getNewMembers(), "dołączyła", "dołączyły", "dołączyło");
                mailService.send(
                        digest.getCoordinatorEmail(),
                        title,
                        "topic-group",
                        Map.of(
                                "lines",
                                List.of("Od ostatniego podsumowania do grupy „%s”, którą koordynujesz, %s %s."
                                        .formatted(digest.getGroupName(), joined, newMembers)),
                                "linkPath",
                                "/dashboard/groups/" + digest.getGroupSlug(),
                                "linkLabel",
                                "Zobacz członków grupy"));
            }
        }
    }

    static String newMembers(long count) {
        return count + " " + plural(count, "nowa osoba", "nowe osoby", "nowych osób");
    }

    /** Same rule as {@code pluralPl} in the frontend's domain package. */
    private static String plural(long count, String one, String few, String many) {
        if (count == 1) {
            return one;
        }
        long lastDigit = count % 10;
        long lastTwoDigits = count % 100;
        return lastDigit >= 2 && lastDigit <= 4 && (lastTwoDigits < 12 || lastTwoDigits > 14) ? few : many;
    }
}
