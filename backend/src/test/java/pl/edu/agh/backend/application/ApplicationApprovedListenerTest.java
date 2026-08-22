package pl.edu.agh.backend.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleAssignmentException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleService;

@ExtendWith(MockitoExtension.class)
class ApplicationApprovedListenerTest {

    @Mock
    private KeycloakRoleService keycloakRoleService;

    @InjectMocks
    private ApplicationApprovedListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "verifiedAlumnRole", "verified-alumn");
    }

    @Test
    void assignsVerifiedAlumnRoleOnApproval() {
        listener.onApplicationApproved(new ApplicationApprovedEvent("kc-user-1"));

        verify(keycloakRoleService).addRealmRole("kc-user-1", "verified-alumn");
    }

    @Test
    void swallowsKeycloakFailureWithoutRethrowing() {
        doThrow(new KeycloakRoleAssignmentException("fail", null))
                .when(keycloakRoleService)
                .addRealmRole(eq("kc-user-2"), eq("verified-alumn"));

        listener.onApplicationApproved(new ApplicationApprovedEvent("kc-user-2"));

        verify(keycloakRoleService).addRealmRole("kc-user-2", "verified-alumn");
        verifyNoMoreInteractions(keycloakRoleService);
    }
}
