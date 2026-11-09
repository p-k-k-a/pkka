package pl.edu.agh.backend.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final List<ApplicationStatus> BLOCKING_STATUSES =
            List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED);

    private final ApplicationRepository applicationRepository;
    private final CallerUserService callerUserService;
    private final UserRepository userRepository;
    private final DiscordAlumnVouch discordAlumnVouch;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ApplicationResponse create(Caller caller, CreateApplicationRequest request, boolean vouchedByDiscord) {
        User applicant = callerUserService.getOrCreate(caller);

        if (applicationRepository.existsByApplicantIdAndStatusIn(applicant.getId(), BLOCKING_STATUSES)) {
            throw new ApplicationAlreadyExistsException();
        }

        Application application = Application.builder()
                .applicant(applicant)
                .faculty(request.faculty())
                .fieldOfStudy(request.fieldOfStudy())
                .studyType(request.studyType())
                .graduationYear(request.graduationYear())
                .meetingPreferences(new ArrayList<>(request.meetingPreferences()))
                .coCreationInterest(request.coCreationInterest())
                .newsletterSubscription(request.newsletterSubscription())
                .phoneNumber(request.phoneNumber())
                .interests(new ArrayList<>(request.interests()))
                .build();

        Instant now = Instant.now();
        request.consents().forEach(type -> application.addConsent(type, now));

        if (vouchedByDiscord) {
            approveByDiscordVouch(application);
        }

        try {
            return ApplicationResponse.from(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationAlreadyExistsException();
        }
    }

    @Transactional
    public void approveIfDiscordVouched(String keycloakId) {
        if (applicationRepository.existsByApplicantKeycloakIdAndStatus(keycloakId, ApplicationStatus.APPROVED)) {
            return;
        }
        userRepository
                .findByKeycloakId(keycloakId)
                .filter(discordAlumnVouch::vouchesFor)
                .map(this::pendingOrEmptyApplication)
                .ifPresent(application -> {
                    approveByDiscordVouch(application);
                    applicationRepository.saveAndFlush(application);
                });
    }

    private Application pendingOrEmptyApplication(User applicant) {
        return applicationRepository
                .findByApplicantIdAndStatus(applicant.getId(), ApplicationStatus.UNDER_REVIEW)
                .orElseGet(() -> Application.builder().applicant(applicant).build());
    }

    private void approveByDiscordVouch(Application application) {
        application.approveByDiscordVouch();
        eventPublisher.publishEvent(
                new ApplicationApprovedEvent(application.getApplicant().getKeycloakId()));
    }

    /** No local row means no application, so this never provisions one the way the write paths do. */
    @Transactional(readOnly = true)
    public ApplicationResponse getMine(Caller caller) {
        return callerUserService
                .find(caller)
                .flatMap(applicant ->
                        applicationRepository.findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId()))
                .map(ApplicationResponse::from)
                .orElseThrow(ApplicationNotFoundException::new);
    }
}
