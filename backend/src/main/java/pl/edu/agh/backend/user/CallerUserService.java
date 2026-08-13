package pl.edu.agh.backend.user;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;

@Service
@RequiredArgsConstructor
public class CallerUserService {

    private final UserProvisioningService userProvisioningService;
    private final UserRepository userRepository;

    /** A valid token proves the account exists, so a missing local row is created rather than refused. */
    @Transactional
    public User getOrCreate(Caller caller) {
        return userProvisioningService.getOrCreate(caller.requireKeycloakId());
    }

    @Transactional(readOnly = true)
    public Optional<User> find(Caller caller) {
        return caller.isAnonymous() ? Optional.empty() : userRepository.findByKeycloakId(caller.keycloakId());
    }
}
