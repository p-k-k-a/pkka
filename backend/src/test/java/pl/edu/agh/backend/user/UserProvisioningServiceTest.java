package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KeycloakUserService keycloakUserService;

    @InjectMocks
    private UserProvisioningService provisioningService;

    @Test
    void provisionIfAbsentReturnsExistingUserWithoutCreating() {
        User existing = new User();
        existing.setKeycloakId("kc-1");
        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existing));

        User result = provisioningService.provisionIfAbsent(new UserPrincipalExtractor.UserPrincipalInfo("kc-1"));

        assertThat(result).isSameAs(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void provisionIfAbsentCreatesUserWhenMissing() {
        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = provisioningService.provisionIfAbsent(new UserPrincipalExtractor.UserPrincipalInfo("kc-new"));

        assertThat(result.getKeycloakId()).isEqualTo("kc-new");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void provisionIfAbsentRecoversFromConcurrentInsertRace() {
        User racedUser = new User();
        racedUser.setKeycloakId("kc-race");

        when(userRepository.findByKeycloakId("kc-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(racedUser));
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        User result = provisioningService.provisionIfAbsent(new UserPrincipalExtractor.UserPrincipalInfo("kc-race"));

        assertThat(result).isSameAs(racedUser);
        verify(userRepository, times(2)).findByKeycloakId("kc-race");
    }

    @Test
    void syncIdentityUpdatesChangedClaimsAndFetchesDiscordIdWhenMissing() {
        User existing = new User();
        existing.setKeycloakId("kc-1");
        existing.setFirstName("Old");
        existing.setLastName("Name");
        existing.setEmail("old@example.com");
        existing.setDiscordId(null);

        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existing));
        when(keycloakUserService.fetchDiscordId("kc-1")).thenReturn(Optional.of("discord-123"));

        provisioningService.syncIdentityFromClaims("kc-1", "New", "Name", "new@example.com");

        assertThat(existing.getFirstName()).isEqualTo("New");
        assertThat(existing.getLastName()).isEqualTo("Name");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getDiscordId()).isEqualTo("discord-123");
    }

    @Test
    void syncIdentitySkipsDiscordFetchWhenAlreadyLinked() {
        User existing = new User();
        existing.setKeycloakId("kc-1");
        existing.setDiscordId("already-linked");

        when(userRepository.findByKeycloakId("kc-1")).thenReturn(Optional.of(existing));

        provisioningService.syncIdentityFromClaims("kc-1", "First", "Last", "mail@example.com");

        verify(keycloakUserService, never()).fetchDiscordId(any());
        assertThat(existing.getDiscordId()).isEqualTo("already-linked");
    }

    @Test
    void syncIdentityIgnoresNullClaimsAndCreatesUserWhenMissing() {
        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(keycloakUserService.fetchDiscordId("kc-new")).thenReturn(Optional.empty());

        provisioningService.syncIdentityFromClaims("kc-new", null, null, null);

        verify(userRepository).save(any(User.class));
        verify(keycloakUserService).fetchDiscordId(eq("kc-new"));
    }
}
