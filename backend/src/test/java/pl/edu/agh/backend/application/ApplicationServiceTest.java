package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import pl.edu.agh.backend.legal.ConsentConflictException;
import pl.edu.agh.backend.legal.ConsentService;
import pl.edu.agh.backend.legal.LegalDocument;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CallerUserService callerUserService;

    @Mock
    private ConsentService consentService;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createPersistsApplicationWithConsents() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        Caller caller = new Caller(keycloakId, Set.of(Roles.USER));

        when(callerUserService.getOrCreate(caller)).thenReturn(applicant);
        when(applicationRepository.existsByApplicantIdAndStatusIn(
                        applicant.getId(), List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)))
                .thenReturn(false);
        when(applicationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        List<LegalDocument> accepted = List.of();
        when(consentService.requireCurrentForApplication(any(), any())).thenReturn(accepted);

        CreateApplicationRequest request = new CreateApplicationRequest(
                Faculty.WI,
                "Informatyka",
                StudyType.MASTER,
                2020,
                Set.of("AI"),
                EnumSet.of(MeetingPreference.ONLINE),
                true,
                "+48123456789",
                Set.of(UUID.randomUUID()));

        ApplicationResponse response = applicationService.create(caller, request);

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.fieldOfStudy()).isEqualTo("Informatyka");
        verify(callerUserService).getOrCreate(caller);
        verify(consentService).recordForApplication(eq(applicant), eq(accepted), any());
    }

    @Test
    void createSavesNothingWhenTheConsentsAreRejected() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        Caller caller = new Caller(keycloakId, Set.of(Roles.USER));
        UUID outdated = UUID.randomUUID();

        when(callerUserService.getOrCreate(caller)).thenReturn(applicant);
        when(consentService.requireCurrentForApplication(any(), any()))
                .thenThrow(ConsentConflictException.outdatedDocument(outdated));

        CreateApplicationRequest request = new CreateApplicationRequest(
                Faculty.WI,
                "Informatyka",
                StudyType.MASTER,
                2020,
                Set.of(),
                EnumSet.noneOf(MeetingPreference.class),
                false,
                "+48123456789",
                Set.of(outdated));

        assertThatThrownBy(() -> applicationService.create(caller, request))
                .isInstanceOf(ConsentConflictException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
        verify(consentService, never()).recordForApplication(any(), any(), any());
    }

    @Test
    void createThrowsWhenBlockingApplicationExists() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        Caller caller = new Caller(keycloakId, Set.of(Roles.USER));

        when(callerUserService.getOrCreate(caller)).thenReturn(applicant);
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
                "+48123456789",
                Set.of(UUID.randomUUID()));

        assertThatThrownBy(() -> applicationService.create(caller, request))
                .isInstanceOf(ApplicationAlreadyExistsException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMineReturnsLatestApplication() {
        String keycloakId = UUID.randomUUID().toString();
        User applicant = new User();
        applicant.setKeycloakId(keycloakId);
        Caller caller = new Caller(keycloakId, Set.of(Roles.USER));
        Application application = Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .build();

        when(callerUserService.find(caller)).thenReturn(Optional.of(applicant));
        when(applicationRepository.findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId()))
                .thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getMine(caller);

        assertThat(response.fieldOfStudy()).isEqualTo("Informatyka");
    }

    @Test
    void getMineDoesNotProvisionAUserRowForACallerWithoutOne() {
        Caller caller = new Caller(UUID.randomUUID().toString(), Set.of(Roles.USER));
        when(callerUserService.find(caller)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getMine(caller)).isInstanceOf(ApplicationNotFoundException.class);
        verify(callerUserService, never()).getOrCreate(any());
    }
}
