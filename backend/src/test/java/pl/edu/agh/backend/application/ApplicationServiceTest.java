package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final List<ApplicationStatus> BLOCKING_STATUSES =
            List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED);

    private static final int GRADUATION_YEAR = 2020;
    private static final String PHONE_NUMBER = "+48123456789";

    private static final boolean VOUCHED_BY_DISCORD = true;
    private static final boolean NOT_VOUCHED_BY_DISCORD = false;

    private final User applicant = applicant();
    private final Caller caller = new Caller(applicant.getKeycloakId(), Set.of(Roles.USER));

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CallerUserService callerUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscordAlumnVouch discordAlumnVouch;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Application> savedApplication;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createPersistsApplicationWithConsents() {
        givenSubmissionIsAllowed();

        ApplicationResponse response = applicationService.create(caller, request(), NOT_VOUCHED_BY_DISCORD);

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.consents())
                .containsExactlyInAnyOrder(ConsentType.REGULATIONS_PRIVACY, ConsentType.GDPR_DATA_PROCESSING);
        assertThat(response.fieldOfStudy()).isEqualTo("Informatyka");
        verify(callerUserService).getOrCreate(caller);
    }

    @Test
    void createThrowsWhenBlockingApplicationExists() {
        when(callerUserService.getOrCreate(caller)).thenReturn(applicant);
        when(applicationRepository.existsByApplicantIdAndStatusIn(applicant.getId(), BLOCKING_STATUSES))
                .thenReturn(true);

        assertThatThrownBy(() -> applicationService.create(caller, request(), NOT_VOUCHED_BY_DISCORD))
                .isInstanceOf(ApplicationAlreadyExistsException.class);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void withoutADiscordVouchTheApplicationWaitsForAReviewer() {
        givenSubmissionIsAllowed();

        ApplicationResponse response = applicationService.create(caller, request(), NOT_VOUCHED_BY_DISCORD);

        assertThat(response.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(response.discordVouched()).isFalse();
        assertThat(response.reviewedAt()).isNull();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void aDiscordVouchApprovesTheApplicationWithoutAReviewer() {
        givenSubmissionIsAllowed();

        ApplicationResponse response = applicationService.create(caller, request(), VOUCHED_BY_DISCORD);

        assertThat(response.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(response.discordVouched()).isTrue();
        assertThat(response.reviewedAt()).isNotNull();
    }

    @Test
    void aDiscordVouchLeavesNoReviewerBehind() {
        givenSubmissionIsAllowed();

        applicationService.create(caller, request(), VOUCHED_BY_DISCORD);

        verify(applicationRepository).saveAndFlush(savedApplication.capture());
        assertThat(savedApplication.getValue().getReviewedBy()).isNull();
    }

    @Test
    void aDiscordVouchAnnouncesTheApprovalSoTheAlumnRoleIsGranted() {
        givenSubmissionIsAllowed();

        applicationService.create(caller, request(), VOUCHED_BY_DISCORD);

        verify(eventPublisher).publishEvent(new ApplicationApprovedEvent(applicant.getKeycloakId()));
    }

    @Test
    void aDiscordVouchCopiesTheGraduationYearOntoTheApplicant() {
        givenSubmissionIsAllowed();

        applicationService.create(caller, request(), VOUCHED_BY_DISCORD);

        assertThat(applicant.getGraduationYear()).isEqualTo(GRADUATION_YEAR);
    }

    @Test
    void aDiscordMemberWithoutAnApplicationIsApprovedAtLoginWithoutTheForm() {
        givenDiscordVouchesAtLogin(Optional.empty());

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verify(applicationRepository).saveAndFlush(savedApplication.capture());
        Application saved = savedApplication.getValue();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(saved.isDiscordVouched()).isTrue();
        assertThat(saved.getFaculty()).isNull();
        assertThat(saved.getConsents()).isEmpty();
    }

    @Test
    void approvalAtLoginAnnouncesItSoTheAlumnRoleIsGranted() {
        givenDiscordVouchesAtLogin(Optional.empty());

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verify(eventPublisher).publishEvent(new ApplicationApprovedEvent(applicant.getKeycloakId()));
    }

    @Test
    void approvalWithoutTheFormKeepsWhatTheProfileAlreadyHas() {
        applicant.setGraduationYear(GRADUATION_YEAR);
        applicant.setPhoneNumber(PHONE_NUMBER);
        givenDiscordVouchesAtLogin(Optional.empty());

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        assertThat(applicant.getGraduationYear()).isEqualTo(GRADUATION_YEAR);
        assertThat(applicant.getPhoneNumber()).isEqualTo(PHONE_NUMBER);
    }

    @Test
    void aPendingApplicationIsApprovedAtLoginOnceDiscordVouches() {
        Application pending = pendingApplication();
        givenDiscordVouchesAtLogin(Optional.of(pending));

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verify(applicationRepository).saveAndFlush(pending);
        assertThat(pending.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(pending.isDiscordVouched()).isTrue();
        assertThat(applicant.getGraduationYear()).isEqualTo(GRADUATION_YEAR);
    }

    @Test
    void withoutADiscordVouchNothingChangesAtLogin() {
        when(userRepository.findByKeycloakId(applicant.getKeycloakId())).thenReturn(Optional.of(applicant));
        when(discordAlumnVouch.vouchesFor(applicant)).thenReturn(false);

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verify(applicationRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void anApprovedAlumnusIsNotCheckedOnDiscordAgain() {
        when(applicationRepository.existsByApplicantKeycloakIdAndStatus(
                        applicant.getKeycloakId(), ApplicationStatus.APPROVED))
                .thenReturn(true);

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verifyNoInteractions(userRepository, discordAlumnVouch, eventPublisher);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void anUnknownAccountIsLeftAloneAtLogin() {
        when(userRepository.findByKeycloakId(applicant.getKeycloakId())).thenReturn(Optional.empty());

        applicationService.approveIfDiscordVouched(applicant.getKeycloakId());

        verifyNoInteractions(discordAlumnVouch, eventPublisher);
        verify(applicationRepository, never()).saveAndFlush(any());
    }

    @Test
    void getMineReturnsLatestApplication() {
        Application application = Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(GRADUATION_YEAR)
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
        when(callerUserService.find(caller)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.getMine(caller)).isInstanceOf(ApplicationNotFoundException.class);
        verify(callerUserService, never()).getOrCreate(any());
    }

    private void givenSubmissionIsAllowed() {
        when(callerUserService.getOrCreate(caller)).thenReturn(applicant);
        when(applicationRepository.existsByApplicantIdAndStatusIn(applicant.getId(), BLOCKING_STATUSES))
                .thenReturn(false);
        when(applicationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Not approved yet is the mock's default: the APPROVED lookup answers false unless stubbed. */
    private void givenDiscordVouchesAtLogin(Optional<Application> pending) {
        when(userRepository.findByKeycloakId(applicant.getKeycloakId())).thenReturn(Optional.of(applicant));
        when(discordAlumnVouch.vouchesFor(applicant)).thenReturn(true);
        when(applicationRepository.findByApplicantIdAndStatus(applicant.getId(), ApplicationStatus.UNDER_REVIEW))
                .thenReturn(pending);
    }

    private Application pendingApplication() {
        return Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(GRADUATION_YEAR)
                .phoneNumber(PHONE_NUMBER)
                .build();
    }

    private static User applicant() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        return user;
    }

    private static CreateApplicationRequest request() {
        return new CreateApplicationRequest(
                Faculty.WI,
                "Informatyka",
                StudyType.MASTER,
                GRADUATION_YEAR,
                Set.of("AI"),
                EnumSet.of(MeetingPreference.ONLINE),
                true,
                false,
                "+48123456789",
                EnumSet.of(ConsentType.REGULATIONS_PRIVACY, ConsentType.GDPR_DATA_PROCESSING));
    }
}
