package pl.edu.agh.backend.infrastructure.keycloak;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakRoleService {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.realm}")
    private String realm;

    public void addRealmRole(String keycloakUserId, String roleName) {
        try {
            RoleRepresentation role =
                    keycloakAdmin.realm(realm).roles().get(roleName).toRepresentation();

            RoleMappingResource mappings =
                    keycloakAdmin.realm(realm).users().get(keycloakUserId).roles();

            boolean alreadyAssigned =
                    mappings.realmLevel().listAll().stream().anyMatch(r -> roleName.equals(r.getName()));
            if (alreadyAssigned) {
                log.debug("User {} already has realm role {}", keycloakUserId, roleName);
                return;
            }

            mappings.realmLevel().add(List.of(role));
            log.info("Added realm role {} to user {}", roleName, keycloakUserId);
        } catch (NotFoundException ex) {
            throw new KeycloakRoleAssignmentException(
                    "Keycloak user or realm role not found: user=%s role=%s".formatted(keycloakUserId, roleName), ex);
        } catch (RuntimeException ex) {
            throw new KeycloakRoleAssignmentException(
                    "Failed to assign realm role %s to user %s".formatted(roleName, keycloakUserId), ex);
        }
    }
}
