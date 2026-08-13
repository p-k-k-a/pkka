package pl.edu.agh.backend.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.user.UserPrincipalExtractor.UserPrincipalInfo;

/** The authenticated caller as a local {@link User} row — one behaviour for every feature that needs it. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserPrincipalExtractor principalExtractor;
    private final UserProvisioningService userProvisioningService;
    private final UserRepository userRepository;

    /**
     * For write paths. A Keycloak token proves the account exists, so a missing local row is created here
     * rather than reported as an error the caller could not fix; an unreadable principal is a 401.
     */
    @Transactional
    public User require(Authentication authentication) {
        return userProvisioningService.provisionIfAbsent(requireInfo(authentication));
    }

    /** For read paths open to anonymous callers: never provisions, so an unknown caller is just empty. */
    @Transactional(readOnly = true)
    public Optional<User> find(Authentication authentication) {
        return principalInfo(authentication).flatMap(info -> userRepository.findByKeycloakId(info.keycloakId()));
    }

    /** For callers that only need the identity, not the row — no database access at all. */
    public String requireKeycloakId(Authentication authentication) {
        return requireInfo(authentication).keycloakId();
    }

    private UserPrincipalInfo requireInfo(Authentication authentication) {
        return principalInfo(authentication).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Optional<UserPrincipalInfo> principalInfo(Authentication authentication) {
        return authentication == null ? Optional.empty() : principalExtractor.extract(authentication);
    }
}
