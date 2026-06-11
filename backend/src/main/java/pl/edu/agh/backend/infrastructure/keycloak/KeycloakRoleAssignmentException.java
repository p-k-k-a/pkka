package pl.edu.agh.backend.infrastructure.keycloak;

public class KeycloakRoleAssignmentException extends RuntimeException {

    public KeycloakRoleAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
