package pl.edu.agh.backend.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final List<ApplicationStatus> BLOCKING_STATUSES =
            List.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED);

    private final ApplicationRepository applicationRepository;
    private final CallerUserService callerUserService;

    @Transactional
    public ApplicationResponse create(Caller caller, CreateApplicationRequest request) {
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

        try {
            return ApplicationResponse.from(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getMine(Caller caller) {
        User applicant = callerUserService.getOrCreate(caller);
        return applicationRepository
                .findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId())
                .map(ApplicationResponse::from)
                .orElseThrow(ApplicationNotFoundException::new);
    }
}
