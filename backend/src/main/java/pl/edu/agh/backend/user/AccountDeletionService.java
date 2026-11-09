package pl.edu.agh.backend.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;
import pl.edu.agh.backend.storage.StorageService;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final EventRegistrationService eventRegistrationService;
    private final KeycloakUserService keycloakUserService;
    private final StorageService storageService;

    @Transactional
    public void deleteAccount(String keycloakId, String email) {
        User user = userRepository
                .findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
        if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email.strip())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail does not match the account");
        }

        eventRegistrationService.cancelUpcomingRegistrations(user);
        userRepository.deleteNow(user.getId());
        keycloakUserService.deleteUser(keycloakId);
        String avatarKey = user.getAvatarKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteAvatar(avatarKey);
            }
        });
    }

    private void deleteAvatar(String key) {
        try {
            storageService.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Account deleted but its avatar {} was left in storage: {}", key, ex.getMessage());
        }
    }
}
