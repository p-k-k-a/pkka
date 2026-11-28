package pl.edu.agh.backend.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.notifications.NotificationService;

@ExtendWith(MockitoExtension.class)
class ApplicationDecisionNotifierTest {

    private static final UUID APPLICANT = UUID.randomUUID();

    @Mock
    private MailService mailService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ApplicationDecisionNotifier notifier;

    @Test
    void approval_isMailedToTheApplicant() {
        notifier.onApplicationReviewed(
                new ApplicationReviewedEvent(APPLICANT, "ala@example.com", "Ala", ApplicationStatus.APPROVED, null));

        verify(mailService)
                .send(
                        "ala@example.com",
                        "Twój wniosek został zaakceptowany",
                        "application-approved",
                        Map.of("firstName", "Ala"));
    }

    @Test
    void rejection_carriesTheReasonLineByLine() {
        notifier.onApplicationReviewed(new ApplicationReviewedEvent(
                APPLICANT, "ala@example.com", null, ApplicationStatus.REJECTED, "Brak dyplomu.\nDołącz skan."));

        verify(mailService)
                .send(
                        "ala@example.com",
                        "Twój wniosek został odrzucony",
                        "application-rejected",
                        Map.of(
                                "firstName",
                                "",
                                "reason",
                                "Brak dyplomu.\nDołącz skan.",
                                "reasonLines",
                                List.of("Brak dyplomu.", "Dołącz skan.")));
    }

    @Test
    void anApplicantWithoutAnAddress_getsNoMail() {
        notifier.onApplicationReviewed(
                new ApplicationReviewedEvent(APPLICANT, " ", "Ala", ApplicationStatus.APPROVED, null));

        verify(mailService, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void anUndecidedStatus_getsNoMail() {
        notifier.onApplicationReviewed(new ApplicationReviewedEvent(
                APPLICANT, "ala@example.com", "Ala", ApplicationStatus.UNDER_REVIEW, null));

        verify(mailService, never()).send(any(), any(), any(), any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void approval_isPushedToTheApplicant() {
        notifier.onApplicationReviewed(
                new ApplicationReviewedEvent(APPLICANT, "ala@example.com", "Ala", ApplicationStatus.APPROVED, null));

        verify(notificationService).pushApplicationApproved(APPLICANT);
    }

    @Test
    void rejection_isPushedEvenWithoutAnAddress() {
        notifier.onApplicationReviewed(
                new ApplicationReviewedEvent(APPLICANT, null, "Ala", ApplicationStatus.REJECTED, "Brak dyplomu."));

        verify(notificationService).pushApplicationRejected(APPLICANT);
    }
}
