package pl.edu.agh.backend.infrastructure.keycloak;

import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    public Optional<UserIdentity> fetchIdentity(String keycloakUserId) {
        try {
            UserRepresentation rep =
                    keycloakAdmin.realm(realm).users().get(keycloakUserId).toRepresentation();
            return Optional.of(new UserIdentity(rep.getFirstName(), rep.getLastName()));
        } catch (NotFoundException ex) {
            log.warn("Keycloak user {} not found when fetching identity", keycloakUserId);
            return Optional.empty();
        } catch (RuntimeException ex) {
            log.warn("Failed to fetch identity for Keycloak user {}: {}", keycloakUserId, ex.getMessage());
            return Optional.empty();
        }
    }

    public record UserIdentity(String firstName, String lastName) {}
}
