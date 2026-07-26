package pl.edu.agh.backend.infrastructure.keycloak;

import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private static final String DISCORD_PROVIDER = "discord";

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    public Optional<UserIdentity> fetchIdentity(String keycloakUserId) {
        try {
            UserRepresentation rep =
                    keycloakAdmin.realm(realm).users().get(keycloakUserId).toRepresentation();
            return Optional.of(new UserIdentity(rep.getFirstName(), rep.getLastName(), rep.getEmail()));
        } catch (NotFoundException ex) {
            log.warn("Keycloak user {} not found when fetching identity", keycloakUserId);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch identity for Keycloak user {}: {}", keycloakUserId, ex.getMessage());
            return Optional.empty();
        }
    }

    public record UserIdentity(String firstName, String lastName, String email) {}

    public Optional<String> fetchDiscordId(String keycloakUserId) {
        try {
            return keycloakAdmin.realm(realm).users().get(keycloakUserId).getFederatedIdentity().stream()
                    .filter(fi -> DISCORD_PROVIDER.equals(fi.getIdentityProvider()))
                    .map(FederatedIdentityRepresentation::getUserId)
                    .findFirst();
        } catch (NotFoundException ex) {
            log.warn("Keycloak user {} not found when fetching Discord identity", keycloakUserId);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch Discord identity for Keycloak user {}: {}", keycloakUserId, ex.getMessage());
            return Optional.empty();
        }
    }
}
