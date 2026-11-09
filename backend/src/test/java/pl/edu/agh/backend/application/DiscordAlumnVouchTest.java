package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DiscordAlumnVouchTest {

    private static final String KEYCLOAK_ID = UUID.randomUUID().toString();
    private static final String DISCORD_ID = "999999999999999999";

    private final Caller caller = new Caller(KEYCLOAK_ID, Set.of(Roles.USER));

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscordGuildMembership guildMembership;

    @InjectMocks
    private DiscordAlumnVouch vouch;

    @Test
    void somebodyDiscordConfirmsIsVouchedFor() {
        given(userRepository.findByKeycloakId(KEYCLOAK_ID)).willReturn(Optional.of(userWithDiscordId(DISCORD_ID)));
        given(guildMembership.holdsAlumnRole(DISCORD_ID)).willReturn(true);

        assertThat(vouch.vouchesFor(caller)).isTrue();
    }

    @Test
    void somebodyDiscordDoesNotConfirmIsNotVouchedFor() {
        given(userRepository.findByKeycloakId(KEYCLOAK_ID)).willReturn(Optional.of(userWithDiscordId(DISCORD_ID)));
        given(guildMembership.holdsAlumnRole(DISCORD_ID)).willReturn(false);

        assertThat(vouch.vouchesFor(caller)).isFalse();
    }

    @Test
    void anAccountWithoutALinkedDiscordIsNotVouchedFor() {
        given(userRepository.findByKeycloakId(KEYCLOAK_ID)).willReturn(Optional.of(userWithDiscordId(null)));

        assertThat(vouch.vouchesFor(caller)).isFalse();
        then(guildMembership).shouldHaveNoInteractions();
    }

    @Test
    void aCallerWithoutALocalRowIsNotVouchedFor() {
        given(userRepository.findByKeycloakId(KEYCLOAK_ID)).willReturn(Optional.empty());

        assertThat(vouch.vouchesFor(caller)).isFalse();
        then(guildMembership).shouldHaveNoInteractions();
    }

    @Test
    void anAnonymousCallerIsNotVouchedFor() {
        assertThat(vouch.vouchesFor(Caller.anonymous())).isFalse();
        then(userRepository).shouldHaveNoInteractions();
        then(guildMembership).shouldHaveNoInteractions();
    }

    @Test
    void aUserAlreadyAtHandIsJudgedWithoutLoadingItAgain() {
        given(guildMembership.holdsAlumnRole(DISCORD_ID)).willReturn(true);

        assertThat(vouch.vouchesFor(userWithDiscordId(DISCORD_ID))).isTrue();
        then(userRepository).shouldHaveNoInteractions();
    }

    private User userWithDiscordId(String discordId) {
        User user = new User();
        user.setKeycloakId(KEYCLOAK_ID);
        user.setDiscordId(discordId);
        return user;
    }
}
