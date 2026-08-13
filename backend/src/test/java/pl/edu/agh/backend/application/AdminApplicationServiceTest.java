package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPrincipalExtractor principalExtractor;

    @Mock
    private UserProvisioningService userProvisioningService;

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

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(principalExtractor.extract(any()))
                .thenReturn(Optional.of(new UserPrincipalExtractor.UserPrincipalInfo(reviewer.getKeycloakId())));
        when(userRepository.findByKeycloakId(reviewer.getKeycloakId())).thenReturn(Optional.of(reviewer));
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response =
                adminApplicationService.approve(jwtAuth(reviewer.getKeycloakId()), applicationId);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(applicant.getGraduationYear()).isEqualTo(2020);

        verify(userProvisioningService).provisionIfAbsent(any());

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

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(principalExtractor.extract(any()))
                .thenReturn(Optional.of(new UserPrincipalExtractor.UserPrincipalInfo(reviewer.getKeycloakId())));
        when(userRepository.findByKeycloakId(reviewer.getKeycloakId())).thenReturn(Optional.of(reviewer));
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        ApplicationResponse response =
                adminApplicationService.reject(jwtAuth(reviewer.getKeycloakId()), applicationId, "Incomplete docs");

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

    private static JwtAuthenticationToken jwtAuth(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
