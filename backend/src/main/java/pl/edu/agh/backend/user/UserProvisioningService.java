package pl.edu.agh.backend.user;

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

    @Transactional
    public void provisionIfAbsent(UserPrincipalExtractor.UserPrincipalInfo info) {
        var userOpt = userRepository.findByKeycloakId(info.keycloakId());
        if (userOpt.isPresent()) {
            var existingUser = userOpt.get();
            syncNames(existingUser, info.keycloakId());
            log.debug(
                    "User provisioning check completed keycloakId={} userId={}",
                    info.keycloakId(),
                    existingUser.getId());
            return;
        }

        try {
            log.info("Provisioning new user keycloakId={}", info.keycloakId());

            var createdUser = new User();
            createdUser.setKeycloakId(info.keycloakId());
            syncNames(createdUser, info.keycloakId());
            var savedUser = userRepository.save(createdUser);
            log.debug(
                    "User provisioning check completed keycloakId={} userId={}", info.keycloakId(), savedUser.getId());
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request created the user; re-load and continue.
            var existingUser =
                    userRepository.findByKeycloakId(info.keycloakId()).orElseThrow(() -> ex);
            log.debug(
                    "User provisioning check completed keycloakId={} userId={}",
                    info.keycloakId(),
                    existingUser.getId());
        }
    }

    private void syncNames(User user, String keycloakId) {
        keycloakUserService.fetchIdentity(keycloakId).ifPresent(identity -> {
            if (identity.firstName() != null) {
                user.setFirstName(identity.firstName());
            }
            if (identity.lastName() != null) {
                user.setLastName(identity.lastName());
            }
        });
    }
}
