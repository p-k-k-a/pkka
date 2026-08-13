package pl.edu.agh.backend.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;

/** The caller of the current request as a local {@link User} row — one behaviour for every feature that needs it. */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserProvisioningService userProvisioningService;
    private final UserRepository userRepository;

    /**
     * For write paths. A Keycloak token proves the account exists, so a missing local row is created
     * here rather than reported as an error the caller could not fix.
     */
    @Transactional
    public User require(Caller caller) {
        return userProvisioningService.provisionIfAbsent(caller.requireKeycloakId());
    }

    /** For read paths open to anonymous callers: never provisions, so an unknown caller is just empty. */
    @Transactional(readOnly = true)
    public Optional<User> find(Caller caller) {
        return caller.isAnonymous() ? Optional.empty() : userRepository.findByKeycloakId(caller.keycloakId());
    }
}
