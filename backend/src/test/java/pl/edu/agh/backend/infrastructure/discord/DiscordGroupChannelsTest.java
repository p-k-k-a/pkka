package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordGroupChannelsTest {

    private static final String GUILD_ID = "111111111111111111";
    private static final String CATEGORY_ID = "666666666666666666";
    private static final String ROLE_ID = "222222222222222222";
    private static final String CHANNEL_ID = "444444444444444444";
    private static final String APPLICATION_ID = "888888888888888888";
    private static final String OWNER_DISCORD_ID = "999999999999999999";
    private static final String BOT_TOKEN = "bot-token";

    private static final String ROLES_URI = "https://discord.com/api/v10/guilds/" + GUILD_ID + "/roles";
    private static final String CHANNELS_URI = "https://discord.com/api/v10/guilds/" + GUILD_ID + "/channels";
    private static final String OWNER_PERMISSION_URI =
            "https://discord.com/api/v10/channels/" + CHANNEL_ID + "/permissions/" + OWNER_DISCORD_ID;

    private final DiscordGuildSettingsRepository guildSettings = mock(DiscordGuildSettingsRepository.class);

    private MockRestServiceServer server;
    private DiscordGroupChannels channels;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        channels = new DiscordGroupChannels(
                builder.build(), new DiscordProperties(BOT_TOKEN, APPLICATION_ID, null), guildSettings);
        DiscordGuildSettings settings = new DiscordGuildSettings(GUILD_ID, null, null);
        settings.configure(Optional.empty(), Optional.empty(), Optional.of(CATEGORY_ID));
        given(guildSettings.findFirstBy()).willReturn(Optional.of(settings));
    }

    @Test
    void aRoleIsCreatedWithoutPermissionsAndKnownByItsId() {
        server.expect(requestTo(ROLES_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andExpect(jsonPath("$.name").value("Grupa AI"))
                .andExpect(jsonPath("$.permissions").value("0"))
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(ROLE_ID), MediaType.APPLICATION_JSON));

        assertThat(channels.createRole("Grupa AI")).contains(ROLE_ID);
        server.verify();
    }

    @Test
    void aNameLongerThanDiscordAllowsIsCut() {
        server.expect(requestTo(ROLES_URI))
                .andExpect(jsonPath("$.name").value("a".repeat(100)))
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(ROLE_ID), MediaType.APPLICATION_JSON));

        channels.createRole("a".repeat(200));

        server.verify();
    }

    @Test
    void aChannelInTheCategoryIsHiddenFromEveryoneButTheRoleAndTheBot() {
        server.expect(requestTo(CHANNELS_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.name").value("grupa-ai"))
                .andExpect(jsonPath("$.parent_id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.permission_overwrites[0].id").value(GUILD_ID))
                .andExpect(jsonPath("$.permission_overwrites[0].deny").value("1024"))
                .andExpect(jsonPath("$.permission_overwrites[1].id").value(ROLE_ID))
                .andExpect(jsonPath("$.permission_overwrites[1].allow").value("1024"))
                .andExpect(jsonPath("$.permission_overwrites[2].id").value(APPLICATION_ID))
                .andExpect(jsonPath("$.permission_overwrites[2].type").value(1))
                .andExpect(jsonPath("$.permission_overwrites[2].allow").value("1024"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\": \"%s\"}".formatted(CHANNEL_ID)));

        assertThat(channels.createChannel("grupa-ai", ROLE_ID)).contains(CHANNEL_ID);
        server.verify();
    }

    @Test
    void theOwnerIsAllowedToManageMessages() {
        server.expect(requestTo(OWNER_PERMISSION_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.type").value(1))
                .andExpect(jsonPath("$.allow").value("8192"))
                .andRespond(withNoContent());

        assertThat(channels.allowManageMessages(CHANNEL_ID, OWNER_DISCORD_ID)).isTrue();
        server.verify();
    }

    @Test
    void aRefusalFromDiscordLeavesNothingCreated() {
        server.expect(requestTo(ROLES_URI)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(channels.createRole("Grupa AI")).isEmpty();
    }

    @Test
    void withoutTheGroupsCategoryNothingIsSentToDiscord() {
        given(guildSettings.findFirstBy()).willReturn(Optional.of(new DiscordGuildSettings(GUILD_ID, null, null)));

        assertThat(channels.isConfigured()).isFalse();
        assertThat(channels.createRole("Grupa AI")).isEmpty();
        server.verify();
    }
}
