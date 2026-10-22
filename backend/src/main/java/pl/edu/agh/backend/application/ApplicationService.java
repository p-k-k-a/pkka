package pl.edu.agh.backend.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.legal.ConsentService;
import pl.edu.agh.backend.legal.LegalDocument;
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
    private final ConsentService consentService;

    @Transactional
    public ApplicationResponse create(Caller caller, CreateApplicationRequest request) {
        User applicant = callerUserService.getOrCreate(caller);
        Instant now = Instant.now();
        List<LegalDocument> acceptedDocuments =
                consentService.requireCurrentForApplication(request.acceptedDocumentIds(), now);

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
                .phoneNumber(request.phoneNumber())
                .interests(new ArrayList<>(request.interests()))
                .build();

        ApplicationResponse response;
        try {
            response = ApplicationResponse.from(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationAlreadyExistsException();
        }
        consentService.recordForApplication(applicant, acceptedDocuments, now);
        return response;
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
