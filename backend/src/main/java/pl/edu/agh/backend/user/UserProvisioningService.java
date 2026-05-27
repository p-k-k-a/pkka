package pl.edu.agh.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserRepository userRepository;

    @Transactional
    public void provisionIfAbsent(UserPrincipalExtractor.UserPrincipalInfo info) {
        var user = userRepository
                .findByKeycloakId(info.keycloakId())
                .orElseGet(() -> {
                    log.info("Provisioning new user keycloakId={}", info.keycloakId());
                    var createdUser = new User();
                    createdUser.setKeycloakId(info.keycloakId());
                    createdUser.setFirstName(info.firstName());
                    createdUser.setLastName(info.lastName());
                    return userRepository.save(createdUser);
                });

        log.debug("User provisioning check completed keycloakId={} userId={}", info.keycloakId(), user.getId());
    }
}