package pl.edu.agh.backend.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final List<ApplicationStatus> OPEN = List.of(ApplicationStatus.UNDER_REVIEW);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserPrincipalExtractor principalExtractor;
    private final UserProvisioningService userProvisioningService;

    @Transactional
    public ApplicationResponseDto create(Authentication authentication, CreateApplicationRequestDto request) {
        User applicant = resolveApplicant(authentication);

        if (applicationRepository.existsByApplicantIdAndStatusIn(applicant.getId(), OPEN)) {
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
            return ApplicationResponseDto.from(applicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException ex) {
            throw new ApplicationAlreadyExistsException();
        }
    }

    @Transactional(readOnly = true)
    public ApplicationResponseDto getMine(Authentication authentication) {
        User applicant = resolveApplicant(authentication);
        return applicationRepository
                .findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId())
                .map(ApplicationResponseDto::from)
                .orElseThrow(ApplicationNotFoundException::new);
    }

    private User resolveApplicant(Authentication authentication) {
        var info = principalExtractor
                .extract(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        userProvisioningService.provisionIfAbsent(info);
        return userRepository
                .findByKeycloakId(info.keycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
