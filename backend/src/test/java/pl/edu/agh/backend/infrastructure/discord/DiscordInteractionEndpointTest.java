package pl.edu.agh.backend.infrastructure.discord;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class DiscordInteractionEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String URI = "/api/public/discord/interactions";
    private static final String TIMESTAMP = "1760000000";
    private static final String GUILD_ID = "111111111111111111";
    private static final String OTHER_GUILD_ID = "121212121212121212";
    private static final String ADMIN_ID = "999999999999999999";
    private static final String CHANNEL_ID = "444444444444444444";
    private static final String ALUMN_ROLE = "222222222222222222";
    private static final String BOT_ROLE = "777777777777777777";
    private static final String MESSAGE_ID = "555555555555555555";

    private static final DiscordInteractionSigner DISCORD = new DiscordInteractionSigner();

    @DynamicPropertySource
    static void trustTheTestKey(DynamicPropertyRegistry registry) {
        registry.add("discord.public-key", DISCORD::publicKeyHex);
    }

    @MockitoBean
    private DiscordMessages discordMessages;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        when(discordMessages.post(any(), any())).thenReturn(Optional.of(MESSAGE_ID));
    }

    @Test
    void anInteractionNotSignedByDiscordIsTurnedAway() throws Exception {
        signedBy(new DiscordInteractionSigner(), command(GUILD_ID, channel(CHANNEL_ID)))
                .andExpect(status().isUnauthorized());

        verify(discordMessages, never()).post(any(), any());
        assertThat(settingsCount()).isZero();
    }

    @Test
    void aPingIsAnsweredWithAPong() throws Exception {
        interact("{\"type\": 1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(1));
    }

    @Test
    void theFirstSetupTiesTheBotToTheServer() throws Exception {
        interact(command(GUILD_ID, channel(CHANNEL_ID), role(ALUMN_ROLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value(4))
                .andExpect(jsonPath("$.data.flags").value(64))
                .andExpect(jsonPath("$.data.content").value(containsString("<#" + CHANNEL_ID + ">")));

        verify(discordMessages).post(eq(CHANNEL_ID), any());
        assertThat(settings())
                .containsEntry("guild_id", GUILD_ID)
                .containsEntry("announcements_channel_id", CHANNEL_ID)
                .containsEntry("alumn_role_id", ALUMN_ROLE);
    }

    @Test
    void anotherServerIsTurnedAway() throws Exception {
        setUpOn(GUILD_ID, CHANNEL_ID, ALUMN_ROLE);

        interact(command(OTHER_GUILD_ID, role(BOT_ROLE)))
                .andExpect(jsonPath("$.data.content").value(containsString("another server")));

        assertThat(settings()).containsEntry("guild_id", GUILD_ID).containsEntry("alumn_role_id", ALUMN_ROLE);
    }

    @Test
    void everyoneCannotBeTheAlumnRole() throws Exception {
        interact(command(GUILD_ID, role(GUILD_ID)))
                .andExpect(jsonPath("$.data.content").value(containsString("@everyone")));

        assertThat(settingsCount()).isZero();
    }

    @Test
    void aRoleDiscordManagesForABotCannotBeTheAlumnRole() throws Exception {
        String botRole = """
                {"type": 2, "guild_id": "%s", "member": {"user": {"id": "%s"}},
                 "data": {"name": "setup", "options": [%s],
                          "resolved": {"roles": {"%s": {"id": "%s", "managed": true}}}}}""".formatted(GUILD_ID, ADMIN_ID, role(BOT_ROLE), BOT_ROLE, BOT_ROLE);

        interact(botRole).andExpect(jsonPath("$.data.content").value(containsString("a bot's role")));

        assertThat(settingsCount()).isZero();
    }

    @Test
    void aChannelTheBotCannotWriteToIsNotSaved() throws Exception {
        when(discordMessages.post(any(), any())).thenReturn(Optional.empty());

        interact(command(GUILD_ID, channel(CHANNEL_ID)))
                .andExpect(jsonPath("$.data.content").value(containsString("can't post in")));

        assertThat(settingsCount()).isZero();
    }

    @Test
    void changingTheRoleKeepsTheChannel() throws Exception {
        setUpOn(GUILD_ID, CHANNEL_ID, null);

        interact(command(GUILD_ID, role(ALUMN_ROLE))).andExpect(status().isOk());

        verify(discordMessages, never()).post(any(), any());
        assertThat(settings())
                .containsEntry("announcements_channel_id", CHANNEL_ID)
                .containsEntry("alumn_role_id", ALUMN_ROLE);
    }

    @Test
    void withoutOptionsTheCurrentSetupIsShownWithoutPingingTheRole() throws Exception {
        setUpOn(GUILD_ID, CHANNEL_ID, ALUMN_ROLE);

        interact(command(GUILD_ID))
                .andExpect(jsonPath("$.data.content").value(containsString("<#" + CHANNEL_ID + ">")))
                .andExpect(jsonPath("$.data.content").value(containsString("<@&" + ALUMN_ROLE + ">")))
                .andExpect(jsonPath("$.data.allowed_mentions.parse").isEmpty());

        verify(discordMessages, never()).post(any(), any());
    }

    private ResultActions interact(String json) throws Exception {
        return signedBy(DISCORD, json);
    }

    private ResultActions signedBy(DiscordInteractionSigner signer, String json) throws Exception {
        byte[] body = json.getBytes(UTF_8);
        return mockMvc.perform(post(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature-Timestamp", TIMESTAMP)
                .header("X-Signature-Ed25519", signer.sign(TIMESTAMP, body))
                .content(body));
    }

    private static String command(String guildId, String... options) {
        return """
                {"type": 2, "guild_id": "%s", "member": {"user": {"id": "%s"}},
                 "data": {"name": "setup", "options": [%s]}}""".formatted(guildId, ADMIN_ID, String.join(", ", options));
    }

    private static String channel(String channelId) {
        return "{\"name\": \"channel\", \"type\": 7, \"value\": \"%s\"}".formatted(channelId);
    }

    private static String role(String roleId) {
        return "{\"name\": \"role\", \"type\": 8, \"value\": \"%s\"}".formatted(roleId);
    }

    private void setUpOn(String guildId, String channelId, String roleId) {
        jdbcTemplate.update(
                "INSERT INTO discord_settings (guild_id, announcements_channel_id, alumn_role_id) VALUES (?, ?, ?)",
                guildId,
                channelId,
                roleId);
    }

    private Map<String, Object> settings() {
        return jdbcTemplate.queryForMap("SELECT * FROM discord_settings");
    }

    private int settingsCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM discord_settings", Integer.class);
    }
}
