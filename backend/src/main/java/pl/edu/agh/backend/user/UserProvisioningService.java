package pl.edu.agh.backend.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;

@Service
@Slf4j
public class UserProvisioningService {

    private static final long MAX_TRACKED_USERS = 100_000;

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;

    /**
     * Presence of an entry means "identity synced within the refresh interval" — expiry does the
     * staleness bookkeeping, and the size bound keeps memory usage flat.
     */
    private final Cache<String, Instant> recentSyncs;

    public UserProvisioningService(
            UserRepository userRepository,
            KeycloakUserService keycloakUserService,
            @Value("${app.identity.refresh-interval}") Duration refreshInterval) {
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.recentSyncs = Caffeine.newBuilder()
                .expireAfterWrite(refreshInterval)
                .maximumSize(MAX_TRACKED_USERS)
                .build();
    }

    /**
     * Ensures a local user row exists for the given Keycloak subject. No external calls — safe to
     * call on hot request paths that only need the row to satisfy foreign keys.
     */
    @Transactional
    public User provisionIfAbsent(UserPrincipalExtractor.UserPrincipalInfo info) {
        return userRepository.findByKeycloakId(info.keycloakId()).orElseGet(() -> createUser(info.keycloakId()));
    }

    /**
     * Applies identity carried by the login ID token — names and e-mail are claims, so no external
     * call is needed for them. The Discord snowflake is never a claim; it is fetched from Keycloak
     * only while still missing (a linked account never changes it).
     */
    @Transactional
    public void syncIdentityAtLogin(UserPrincipalExtractor.UserPrincipalInfo info) {
        User user = findOrCreate(info.keycloakId());
        applyIdentity(user, info.firstName(), info.lastName(), info.email());
        if (user.getDiscordId() == null) {
            keycloakUserService.fetchDiscordId(info.keycloakId()).ifPresent(user::setDiscordId);
        }
        recentSyncs.put(info.keycloakId(), Instant.now());
    }

    /**
     * Keeps the local identity copy fresh for sessions that never re-login (mobile refresh tokens).
     * Access tokens deliberately carry no identity claims, so data comes from Keycloak — at most
     * once per refresh interval per user; between attempts this method is a cheap in-memory check.
     */
    @Transactional
    public void refreshIdentityIfStale(String keycloakId) {
        if (!dueForRefresh(keycloakId)) {
            return;
        }
        User user = findOrCreate(keycloakId);
        keycloakUserService
                .fetchIdentity(keycloakId)
                .ifPresent(
                        identity -> applyIdentity(user, identity.firstName(), identity.lastName(), identity.email()));
        if (user.getDiscordId() == null) {
            keycloakUserService.fetchDiscordId(keycloakId).ifPresent(user::setDiscordId);
        }
    }

    private boolean dueForRefresh(String keycloakId) {
        if (recentSyncs.getIfPresent(keycloakId) != null) {
            return false;
        }
        recentSyncs.put(keycloakId, Instant.now());
        return true;
    }

    private void applyIdentity(User user, String firstName, String lastName, String email) {
        if (firstName != null && !Objects.equals(user.getFirstName(), firstName)) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !Objects.equals(user.getLastName(), lastName)) {
            user.setLastName(lastName);
        }
        if (email != null && !Objects.equals(user.getEmail(), email)) {
            user.setEmail(email);
        }
    }

    private User findOrCreate(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).orElseGet(() -> createUser(keycloakId));
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
