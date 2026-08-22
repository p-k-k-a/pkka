package pl.edu.agh.backend.infrastructure.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private Keycloak keycloakAdmin;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "realm", "pkka");
    }

    @Test
    void fetchDiscordIdReturnsDiscordProviderUserId() {
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        FederatedIdentityRepresentation discord = new FederatedIdentityRepresentation();
        discord.setIdentityProvider("discord");
        discord.setUserId("123456789012345678");

        FederatedIdentityRepresentation google = new FederatedIdentityRepresentation();
        google.setIdentityProvider("google");
        google.setUserId("google-id");

        when(keycloakAdmin.realm("pkka")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-1")).thenReturn(userResource);
        when(userResource.getFederatedIdentity()).thenReturn(List.of(google, discord));

        assertThat(keycloakUserService.fetchDiscordId("kc-1")).contains("123456789012345678");
    }

    @Test
    void fetchDiscordIdReturnsEmptyWhenUserNotFound() {
        when(keycloakAdmin.realm("pkka")).thenThrow(new NotFoundException());

        assertThat(keycloakUserService.fetchDiscordId("missing")).isEmpty();
    }

    @Test
    void fetchDiscordIdReturnsEmptyOnRuntimeFailure() {
        when(keycloakAdmin.realm("pkka")).thenThrow(new RuntimeException("timeout"));

        assertThat(keycloakUserService.fetchDiscordId("kc-1")).isEmpty();
    }

    @Test
    void fetchDiscordIdReturnsEmptyWhenNoDiscordLink() {
        RealmResource realm = mock(RealmResource.class);
        UsersResource users = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);

        when(keycloakAdmin.realm("pkka")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(users.get("kc-1")).thenReturn(userResource);
        when(userResource.getFederatedIdentity()).thenReturn(List.of());

        assertThat(keycloakUserService.fetchDiscordId("kc-1")).isEmpty();
    }
}
