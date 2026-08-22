package pl.edu.agh.backend.infrastructure.keycloak;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KeycloakRoleServiceTest {

    @Mock
    private Keycloak keycloakAdmin;

    @InjectMocks
    private KeycloakRoleService keycloakRoleService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakRoleService, "realm", "pkka");
    }

    @Test
    void addRealmRoleAssignsWhenNotYetPresent() {
        RealmResource realm = mock(RealmResource.class);
        RolesResource roles = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource mappings = mock(RoleMappingResource.class);
        var realmLevel = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);

        RoleRepresentation role = new RoleRepresentation();
        role.setName("verified-alumn");

        when(keycloakAdmin.realm("pkka")).thenReturn(realm);
        when(realm.roles()).thenReturn(roles);
        when(roles.get("verified-alumn")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(role);
        when(realm.users()).thenReturn(users);
        when(users.get("user-1")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(mappings);
        when(mappings.realmLevel()).thenReturn(realmLevel);
        when(realmLevel.listAll()).thenReturn(List.of());
        doNothing().when(realmLevel).add(any());

        keycloakRoleService.addRealmRole("user-1", "verified-alumn");

        verify(realmLevel).add(List.of(role));
    }

    @Test
    void addRealmRoleSkipsWhenAlreadyAssigned() {
        RealmResource realm = mock(RealmResource.class);
        RolesResource roles = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource mappings = mock(RoleMappingResource.class);
        var realmLevel = mock(org.keycloak.admin.client.resource.RoleScopeResource.class);

        RoleRepresentation existing = new RoleRepresentation();
        existing.setName("verified-alumn");

        when(keycloakAdmin.realm("pkka")).thenReturn(realm);
        when(realm.roles()).thenReturn(roles);
        when(roles.get("verified-alumn")).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(existing);
        when(realm.users()).thenReturn(users);
        when(users.get("user-1")).thenReturn(userResource);
        when(userResource.roles()).thenReturn(mappings);
        when(mappings.realmLevel()).thenReturn(realmLevel);
        when(realmLevel.listAll()).thenReturn(List.of(existing));

        keycloakRoleService.addRealmRole("user-1", "verified-alumn");

        verify(realmLevel, never()).add(any());
    }

    @Test
    void addRealmRoleWrapsNotFoundException() {
        when(keycloakAdmin.realm("pkka")).thenThrow(new NotFoundException());

        assertThatThrownBy(() -> keycloakRoleService.addRealmRole("missing", "verified-alumn"))
                .isInstanceOf(KeycloakRoleAssignmentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void addRealmRoleWrapsRuntimeException() {
        when(keycloakAdmin.realm("pkka")).thenThrow(new RuntimeException("network"));

        assertThatThrownBy(() -> keycloakRoleService.addRealmRole("user-1", "verified-alumn"))
                .isInstanceOf(KeycloakRoleAssignmentException.class)
                .hasMessageContaining("Failed to assign");
    }
}
