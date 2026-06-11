package pl.edu.agh.backend.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final UserPrincipalExtractor principalExtractor;
    private final UserProvisioningService userProvisioningService;
    private final KeycloakRoleService keycloakRoleService;

    @Value("${keycloak.verified-alumn-role:verified-alumn}")
    private String verifiedAlumnRole;

    @Transactional(readOnly = true)
    public Page<AdminApplicationResponseDto> list(ApplicationStatus status, Pageable pageable) {
        return applicationRepository
                .findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(AdminApplicationResponseDto::from);
    }

    @Transactional
    public ApplicationResponseDto approve(Authentication authentication, UUID applicationId) {
        User reviewer = resolveReviewer(authentication);
        Application application =
                applicationRepository.findById(applicationId).orElseThrow(ApplicationNotFoundException::new);

        application.approve(reviewer);
        keycloakRoleService.addRealmRole(application.getApplicant().getKeycloakId(), verifiedAlumnRole);

        return ApplicationResponseDto.from(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public ApplicationResponseDto reject(Authentication authentication, UUID applicationId, String reason) {
        User reviewer = resolveReviewer(authentication);
        Application application =
                applicationRepository.findById(applicationId).orElseThrow(ApplicationNotFoundException::new);

        application.reject(reviewer, reason);
        return ApplicationResponseDto.from(applicationRepository.saveAndFlush(application));
    }

    private User resolveReviewer(Authentication authentication) {
        var info = principalExtractor
                .extract(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        userProvisioningService.provisionIfAbsent(info);
        return userRepository
                .findByKeycloakId(info.keycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
