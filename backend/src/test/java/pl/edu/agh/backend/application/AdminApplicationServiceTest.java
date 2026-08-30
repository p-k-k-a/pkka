package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AdminApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CallerUserService callerUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminApplicationService adminApplicationService;

    @Test
    void approvePublishesApplicationApprovedEvent() {
        UUID applicationId = UUID.randomUUID();
        User applicant = user("applicant-kc");
        User reviewer = user("reviewer-kc");
        Application application = pendingApplication(applicant);
        Caller reviewerCaller = caller(reviewer);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(callerUserService.getOrCreate(reviewerCaller)).thenReturn(reviewer);
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = adminApplicationService.approve(reviewerCaller, applicationId);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(applicant.getGraduationYear()).isEqualTo(2020);

        verify(callerUserService).getOrCreate(reviewerCaller);

        ArgumentCaptor<ApplicationApprovedEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationApprovedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().applicantKeycloakId()).isEqualTo("applicant-kc");
    }

    @Test
    void rejectPersistsReasonWithoutPublishingApprovalEvent() {
        UUID applicationId = UUID.randomUUID();
        User applicant = user("applicant-kc");
        User reviewer = user("reviewer-kc");
        Application application = pendingApplication(applicant);
        Caller reviewerCaller = caller(reviewer);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(callerUserService.getOrCreate(reviewerCaller)).thenReturn(reviewer);
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response = adminApplicationService.reject(reviewerCaller, applicationId, "Incomplete docs");

        assertThat(response.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Incomplete docs");
        verify(eventPublisher, org.mockito.Mockito.never()).publishEvent(any());
    }

    @Test
    void getThrowsWhenApplicationMissing() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminApplicationService.get(applicationId))
                .isInstanceOf(ApplicationNotFoundException.class);
    }

    private static Application pendingApplication(User applicant) {
        return Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .build();
    }

    private static User user(String keycloakId) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        return user;
    }

    private static Caller caller(User user) {
        return new Caller(user.getKeycloakId(), Set.of(Roles.ADMIN));
    }
}
