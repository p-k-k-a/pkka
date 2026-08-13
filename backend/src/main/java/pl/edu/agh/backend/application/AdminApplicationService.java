package pl.edu.agh.backend.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CurrentUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<AdminApplicationResponse> list(ApplicationStatus status, Pageable pageable) {
        return applicationRepository
                .findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(AdminApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminApplicationResponse get(UUID applicationId) {
        return applicationRepository
                .findById(applicationId)
                .map(AdminApplicationResponse::from)
                .orElseThrow(ApplicationNotFoundException::new);
    }

    @Transactional
    public ApplicationResponse approve(Caller caller, UUID applicationId) {
        User reviewer = currentUserService.require(caller);
        Application application =
                applicationRepository.findById(applicationId).orElseThrow(ApplicationNotFoundException::new);

        application.approve(reviewer);
        eventPublisher.publishEvent(
                new ApplicationApprovedEvent(application.getApplicant().getKeycloakId()));

        return ApplicationResponse.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public ApplicationResponse reject(Caller caller, UUID applicationId, String reason) {
        User reviewer = currentUserService.require(caller);
        Application application =
                applicationRepository.findById(applicationId).orElseThrow(ApplicationNotFoundException::new);

        application.reject(reviewer, reason);
        return ApplicationResponse.from(applicationRepository.saveAndFlush(application));
    }
}
