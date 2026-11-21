package pl.edu.agh.backend.discord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.infrastructure.discord.DiscordInvites;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class DiscordStatusEndpointTest {

    private static final String MY_DISCORD = "/api/me/discord";
    private static final String INVITE_URL = "https://discord.gg/abc123";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private DiscordGuildMembership guildMembership;

    @MockitoBean
    private DiscordRoleSync roleSync;

    @MockitoBean
    private DiscordInvites invites;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User anna;

    @BeforeEach
    void setUp() {
        anna = user(String.valueOf(System.nanoTime()));
        given(guildMembership.isConfigured()).willReturn(true);
        given(invites.inviteUrl()).willReturn(Optional.of(INVITE_URL));
    }

    @Test
    void userWithoutDiscordIsNotLinked() throws Exception {
        statusOf(user(null))
                .andExpect(jsonPath("$.status").value("NOT_LINKED"))
                .andExpect(jsonPath("$.inviteUrl").doesNotExist());

        verify(roleSync, never()).sync(any());
    }

    @Test
    void memberOfTheServerGetsTheirRolesSyncedAndNoInvite() throws Exception {
        given(roleSync.sync(any())).willReturn(DiscordStatus.ON_SERVER);

        statusOf(anna)
                .andExpect(jsonPath("$.status").value("ON_SERVER"))
                .andExpect(jsonPath("$.inviteUrl").doesNotExist());

        verify(roleSync).sync(anna);
    }

    @Test
    void userOutsideTheServerGetsAnInvite() throws Exception {
        given(roleSync.sync(any())).willReturn(DiscordStatus.NOT_ON_SERVER);

        statusOf(anna)
                .andExpect(jsonPath("$.status").value("NOT_ON_SERVER"))
                .andExpect(jsonPath("$.inviteUrl").value(INVITE_URL));
    }

    @Test
    void unreachableDiscordIsUnknownRatherThanAnError() throws Exception {
        given(roleSync.sync(any())).willThrow(new RestClientException("Discord is down"));

        statusOf(anna).andExpect(jsonPath("$.status").value("UNKNOWN"));
    }

    @Test
    void discordThatIsNotSetUpIsUnknown() throws Exception {
        given(guildMembership.isConfigured()).willReturn(false);

        statusOf(anna).andExpect(jsonPath("$.status").value("UNKNOWN"));

        verify(roleSync, never()).sync(any());
    }

    @Test
    void asksDiscordAtMostOnceAMinutePerUser() throws Exception {
        given(roleSync.sync(any())).willReturn(DiscordStatus.ON_SERVER);

        statusOf(anna);
        statusOf(anna).andExpect(jsonPath("$.status").value("ON_SERVER"));

        verify(roleSync, times(1)).sync(any());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get(MY_DISCORD)).andExpect(status().isUnauthorized());
    }

    private ResultActions statusOf(User user) throws Exception {
        return mockMvc.perform(get(MY_DISCORD).with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk());
    }

    private User user(String discordId) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setDiscordId(discordId);
        return userRepository.saveAndFlush(user);
    }
}
