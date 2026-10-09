package pl.edu.agh.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts a user row in a transaction of its own. Ids are generated in Java, so a plain {@code save()} only
 * writes at flush or commit: a concurrent duplicate would then fail the caller's whole transaction — or be
 * mistaken for whatever constraint the caller was guarding when it flushed. Committing here keeps a lost race
 * contained to this insert, and the winner's row is visible to the caller right after.
 */
@Component
@RequiredArgsConstructor
class UserInserter {

    private final UserRepository userRepository;

    /** Fails with a {@code DataIntegrityViolationException} when the row already exists. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(String keycloakId) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        userRepository.saveAndFlush(user);
    }
}
