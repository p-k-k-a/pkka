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
        userRepository
                .findByKeycloakId(info.keycloakId())
                .orElseGet(() -> {
                    log.info("Provisioning new user keycloakId={}", info.keycloakId());
                    var user = new User();
                    user.setKeycloakId(info.keycloakId());
                    user.setFirstName(info.firstName());
                    user.setLastName(info.lastName());
                    return userRepository.save(user);
                });
    }
}