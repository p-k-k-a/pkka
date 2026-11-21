package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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

class DiscordInvitesTest {

    private static final String GUILD_ID = "111111111111111111";
    private static final String CHANNEL_ID = "444444444444444444";
    private static final String BOT_TOKEN = "bot-token";
    private static final String INVITES_URI = "https://discord.com/api/v10/channels/" + CHANNEL_ID + "/invites";

    private final DiscordGuildSettingsRepository guildSettings = mock(DiscordGuildSettingsRepository.class);

    private MockRestServiceServer server;
    private DiscordInvites invites;
    private DiscordGuildSettings settings;

    @BeforeEach
    void setUp() {
        invites = invitesFor(BOT_TOKEN);
        settings = new DiscordGuildSettings(GUILD_ID, CHANNEL_ID, null);
        given(guildSettings.findFirstBy()).willReturn(Optional.of(settings));
    }

    @Test
    void createsAPermanentInviteToTheAnnouncementsChannelAndKeepsIt() {
        server.expect(requestTo(INVITES_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andExpect(content().json("{\"max_age\": 0, \"max_uses\": 0}"))
                .andRespond(withSuccess("{\"code\": \"abc123\"}", MediaType.APPLICATION_JSON));

        assertThat(invites.inviteUrl()).contains("https://discord.gg/abc123");

        server.verify();
        assertThat(settings.getInviteCode()).contains("abc123");
        verify(guildSettings).save(settings);
    }

    @Test
    void reusesTheSavedInvite() {
        settings.setInviteCode("abc123");

        assertThat(invites.inviteUrl()).contains("https://discord.gg/abc123");

        server.verify();
    }

    @Test
    void settingTheAnnouncementsChannelAgainDropsTheInvite() {
        settings.setInviteCode("abc123");

        settings.configure(Optional.of(CHANNEL_ID), Optional.empty(), Optional.empty());

        assertThat(settings.getInviteCode()).isEmpty();
    }

    @Test
    void settingOnlyTheRoleKeepsTheInvite() {
        settings.setInviteCode("abc123");

        settings.configure(Optional.empty(), Optional.of("222222222222222222"), Optional.empty());

        assertThat(settings.getInviteCode()).contains("abc123");
    }

    @Test
    void noAnnouncementsChannelMeansNoInvite() {
        given(guildSettings.findFirstBy()).willReturn(Optional.of(new DiscordGuildSettings(GUILD_ID, null, null)));

        assertThat(invites.inviteUrl()).isEmpty();

        server.verify();
    }

    @Test
    void refusedInviteIsNotSaved() {
        server.expect(requestTo(INVITES_URI)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThat(invites.inviteUrl()).isEmpty();

        verify(guildSettings, never()).save(any());
    }

    @Test
    void withoutABotTokenNothingIsAskedOfDiscord() {
        assertThat(invitesFor("").inviteUrl()).isEmpty();

        server.verify();
    }

    private DiscordInvites invitesFor(String botToken) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new DiscordInvites(builder.build(), new DiscordProperties(botToken, null, null), guildSettings);
    }
}
