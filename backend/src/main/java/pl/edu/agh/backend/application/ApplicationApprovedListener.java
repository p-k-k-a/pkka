package pl.edu.agh.backend.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleAssignmentException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationApprovedListener {

    private final KeycloakRoleService keycloakRoleService;

    @Value("${keycloak.verified-alumn-role:verified-alumn}")
    private String verifiedAlumnRole;

    @TransactionalEventListener
    public void onApplicationApproved(ApplicationApprovedEvent event) {
        try {
            keycloakRoleService.addRealmRole(event.applicantKeycloakId(), verifiedAlumnRole);
        } catch (KeycloakRoleAssignmentException ex) {
            log.error(
                    "Application approved but assigning role {} to user {} failed",
                    verifiedAlumnRole,
                    event.applicantKeycloakId(),
                    ex);
        }
    }
}
