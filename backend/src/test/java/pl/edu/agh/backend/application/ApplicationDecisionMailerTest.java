package pl.edu.agh.backend.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.infrastructure.mail.MailService;

@ExtendWith(MockitoExtension.class)
class ApplicationDecisionMailerTest {

    @Mock
    private MailService mailService;

    @InjectMocks
    private ApplicationDecisionMailer mailer;

    @Test
    void approval_isMailedToTheApplicant() {
        mailer.onApplicationReviewed(
                new ApplicationReviewedEvent("ala@example.com", "Ala", ApplicationStatus.APPROVED, null));

        verify(mailService)
                .send(
                        "ala@example.com",
                        "Twój wniosek został zaakceptowany",
                        "application-approved",
                        Map.of("firstName", "Ala"));
    }

    @Test
    void rejection_carriesTheReasonLineByLine() {
        mailer.onApplicationReviewed(new ApplicationReviewedEvent(
                "ala@example.com", null, ApplicationStatus.REJECTED, "Brak dyplomu.\nDołącz skan."));

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
        mailer.onApplicationReviewed(new ApplicationReviewedEvent(" ", "Ala", ApplicationStatus.APPROVED, null));

        verify(mailService, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void anUndecidedStatus_getsNoMail() {
        mailer.onApplicationReviewed(
                new ApplicationReviewedEvent("ala@example.com", "Ala", ApplicationStatus.UNDER_REVIEW, null));

        verify(mailService, never()).send(any(), any(), any(), any());
    }
}
