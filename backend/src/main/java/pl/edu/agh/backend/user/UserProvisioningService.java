package pl.edu.agh.backend.user;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;

    /**
     * Ensures a local user row exists for the given Keycloak subject. No external calls — safe to
     * call on hot request paths that only need the row to satisfy foreign keys.
     */
    @Transactional
    public User getOrCreate(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId).orElseGet(() -> createUser(keycloakId));
        log.debug("User provisioning check completed keycloakId={} userId={}", keycloakId, user.getId());
        return user;
    }

    /**
     * Refreshes identity from the ID token at login. First/last name and e-mail are OIDC claims, so
     * no admin call is needed. The Discord snowflake is not a claim; it is fetched from Keycloak only
     * while still missing (a linked account never changes it).
     */
    @Transactional
    public void syncIdentityFromClaims(String keycloakId, String firstName, String lastName, String email) {
        User user = userRepository.findByKeycloakId(keycloakId).orElseGet(() -> createUser(keycloakId));

        if (firstName != null && !Objects.equals(user.getFirstName(), firstName)) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !Objects.equals(user.getLastName(), lastName)) {
            user.setLastName(lastName);
        }
        if (email != null && !Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
        }
        if (user.getDiscordId() == null) {
            keycloakUserService.fetchDiscordId(keycloakId).ifPresent(user::setDiscordId);
        }
    }

    private User createUser(String keycloakId) {
        try {
            log.info("Provisioning new user keycloakId={}", keycloakId);
            User user = new User();
            user.setKeycloakId(keycloakId);
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request created the user; re-load and continue.
            return userRepository.findByKeycloakId(keycloakId).orElseThrow(() -> ex);
        }
    }
}
