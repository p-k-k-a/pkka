package pl.edu.agh.backend.application;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.notifications.NotificationService;

@Component
@RequiredArgsConstructor
public class ApplicationDecisionNotifier {

    private final MailService mailService;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener
    public void onApplicationReviewed(ApplicationReviewedEvent reviewed) {
        switch (reviewed.decision()) {
            case APPROVED -> notificationService.pushApplicationApproved(reviewed.applicantId());
            case REJECTED -> notificationService.pushApplicationRejected(reviewed.applicantId());
            default -> {}
        }
        if (!StringUtils.hasText(reviewed.applicantEmail())) {
            return;
        }
        String firstName = reviewed.applicantFirstName() == null ? "" : reviewed.applicantFirstName();
        switch (reviewed.decision()) {
            case APPROVED ->
                mailService.send(
                        reviewed.applicantEmail(),
                        "Twój wniosek został zaakceptowany",
                        "application-approved",
                        Map.of("firstName", firstName));
            case REJECTED -> {
                String reason = reviewed.rejectionReason() == null ? "" : reviewed.rejectionReason();
                mailService.send(
                        reviewed.applicantEmail(),
                        "Twój wniosek został odrzucony",
                        "application-rejected",
                        Map.of(
                                "firstName",
                                firstName,
                                "reason",
                                reason,
                                "reasonLines",
                                reason.lines().toList()));
            }
            default -> {}
        }
    }
}
