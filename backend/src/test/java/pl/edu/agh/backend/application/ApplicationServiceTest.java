package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPrincipalExtractor principalExtractor;

    @Mock
    private UserProvisioningService userProvisioningService;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createPersistsApplicationWithConsents() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        var auth = jwtAuth(keycloakId);

        when(principalExtractor.extract(auth))
                .thenReturn(Optional.of(new UserPrincipalExtractor.UserPrincipalInfo(keycloakId)));
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(applicant));
        when(applicationRepository.existsByApplicantIdAndStatusIn(
                        applicant.getId(), List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)))
                .thenReturn(false);
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateApplicationRequest request = new CreateApplicationRequest(
                Faculty.WI,
                "Informatyka",
                StudyType.MASTER,
                2020,
                Set.of("AI"),
                EnumSet.of(MeetingPreference.ONLINE),
                true,
                false,
                "+48123456789",
                EnumSet.of(ConsentType.REGULATIONS_PRIVACY, ConsentType.GDPR_DATA_PROCESSING));

        ApplicationResponse response = applicationService.create(auth, request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.fieldOfStudy()).isEqualTo("Informatyka");
        verify(userProvisioningService).provisionIfAbsent(new UserPrincipalExtractor.UserPrincipalInfo(keycloakId));
    }

    @Test
    void createThrowsWhenBlockingApplicationExists() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        var auth = jwtAuth(keycloakId);

        when(principalExtractor.extract(auth))
                .thenReturn(Optional.of(new UserPrincipalExtractor.UserPrincipalInfo(keycloakId)));
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(applicant));
        when(applicationRepository.existsByApplicantIdAndStatusIn(
                        applicant.getId(), List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)))
                .thenReturn(true);

        CreateApplicationRequest request = new CreateApplicationRequest(
                Faculty.WI,
                "Informatyka",
                StudyType.MASTER,
                2020,
                Set.of(),
                EnumSet.noneOf(MeetingPreference.class),
                false,
                false,
                "+48123456789",
                EnumSet.of(ConsentType.REGULATIONS_PRIVACY, ConsentType.GDPR_DATA_PROCESSING));

        assertThatThrownBy(() -> applicationService.create(auth, request))
                .isInstanceOf(ApplicationAlreadyExistsException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMineReturnsLatestApplication() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        var auth = jwtAuth(keycloakId);
        Application application = Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .build();

        when(principalExtractor.extract(auth))
                .thenReturn(Optional.of(new UserPrincipalExtractor.UserPrincipalInfo(keycloakId)));
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(applicant));
        when(applicationRepository.findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId()))
                .thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getMine(auth);

        assertThat(response.fieldOfStudy()).isEqualTo("Informatyka");
    }

    private static JwtAuthenticationToken jwtAuth(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();
        return new JwtAuthenticationToken(jwt, List.of());
    }
}
