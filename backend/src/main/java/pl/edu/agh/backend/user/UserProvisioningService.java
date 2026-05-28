package pl.edu.agh.backend.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserRepository userRepository;

    @Transactional
    public void provisionIfAbsent(UserPrincipalExtractor.UserPrincipalInfo info) {
        var userOpt = userRepository.findByKeycloakId(info.keycloakId());
        if (userOpt.isPresent()) {
            var existingUser = userOpt.get();
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
            createdUser.setFirstName(info.firstName());
            createdUser.setLastName(info.lastName());
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
}
