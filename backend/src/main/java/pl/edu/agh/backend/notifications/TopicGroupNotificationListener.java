package pl.edu.agh.backend.notifications;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.topicgroup.TopicGroup;
import pl.edu.agh.backend.topicgroup.TopicGroupCreatedEvent;
import pl.edu.agh.backend.topicgroup.TopicGroupRepository;
import pl.edu.agh.backend.topicgroup.TopicGroupRequestRejectedEvent;
import pl.edu.agh.backend.user.UserRepository;

/**
 * Tells the alumn who asked for a topic group how the admin decided. Push and mail are separate listeners, as
 * in {@link EventNotificationListener}: both run after commit, so a failed send never undoes the decision.
 */
@Component
@RequiredArgsConstructor
public class TopicGroupNotificationListener {

    private static final int PUSH_REASON_LIMIT = 150;

    private final TopicGroupRepository topicGroupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MailService mailService;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pushGroupCreated(TopicGroupCreatedEvent created) {
        topicGroupRepository
                .findWithCoordinatorById(created.groupId())
                .filter(group -> group.getCoordinator() != null)
                .ifPresent(group -> notificationService.pushToUser(
                        group.getCoordinator().getId(),
                        NotificationType.TOPIC_GROUP_CREATED,
                        createdTitle(group),
                        "Jesteś jej koordynatorem.",
                        group.getSlug()));
    }

    @Async
    @TransactionalEventListener
    public void mailGroupCreated(TopicGroupCreatedEvent created) {
        topicGroupRepository
                .findWithCoordinatorById(created.groupId())
                .filter(group -> group.getCoordinator() != null)
                .ifPresent(group -> mail(
                        group.getCoordinator().getEmail(),
                        createdTitle(group),
                        List.of(
                                "Administrator zatwierdził Twój wniosek. Grupa „%s” już działa, a Ty jesteś jej koordynatorem."
                                        .formatted(group.getName())),
                        "/dashboard/groups/" + group.getSlug(),
                        "Przejdź do grupy"));
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pushRequestRejected(TopicGroupRequestRejectedEvent rejected) {
        notificationService.pushToUser(
                rejected.requesterId(),
                NotificationType.TOPIC_GROUP_REQUEST_REJECTED,
                rejectedTitle(rejected),
                "Powód: " + shorten(rejected.reason()),
                rejected.requestId().toString());
    }

    @Async
    @TransactionalEventListener
    public void mailRequestRejected(TopicGroupRequestRejectedEvent rejected) {
        userRepository
                .findById(rejected.requesterId())
                .ifPresent(requester -> mail(
                        requester.getEmail(),
                        rejectedTitle(rejected),
                        List.of(
                                "Administrator odrzucił Twój wniosek o utworzenie grupy „%s”. Powód:"
                                        .formatted(rejected.groupName()),
                                rejected.reason(),
                                "Możesz poprawić propozycję i złożyć nowy wniosek."),
                        "/dashboard/groups",
                        "Przejdź do grup"));
    }

    private void mail(String email, String subject, List<String> lines, String linkPath, String linkLabel) {
        if (StringUtils.hasText(email)) {
            mailService.send(
                    email,
                    subject,
                    "topic-group",
                    Map.of("lines", lines, "linkPath", linkPath, "linkLabel", linkLabel));
        }
    }

    private static String createdTitle(TopicGroup group) {
        return "Twoja grupa „%s” została utworzona".formatted(group.getName());
    }

    private static String rejectedTitle(TopicGroupRequestRejectedEvent rejected) {
        return "Wniosek o grupę „%s” odrzucony".formatted(rejected.groupName());
    }

    /** The full reason goes by mail; a push shows one short line of it. */
    private static String shorten(String reason) {
        String oneLine = reason.strip().replaceAll("\\s+", " ");
        if (oneLine.length() <= PUSH_REASON_LIMIT) {
            return oneLine;
        }
        int end = PUSH_REASON_LIMIT - 1;
        if (Character.isHighSurrogate(oneLine.charAt(end - 1))) {
            end--;
        }
        return oneLine.substring(0, end).strip() + "…";
    }
}
