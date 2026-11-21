package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class DiscordInvites {

    private static final String INVITES_URI = "https://discord.com/api/v10/channels/{channelId}/invites";
    private static final String INVITE_URL = "https://discord.gg/";

    private final RestClient restClient;
    private final DiscordProperties discord;
    private final DiscordGuildSettingsRepository guildSettings;

    public DiscordInvites(
            @Qualifier("discordRestClient") RestClient restClient,
            DiscordProperties discord,
            DiscordGuildSettingsRepository guildSettings) {
        this.restClient = restClient;
        this.discord = discord;
        this.guildSettings = guildSettings;
    }

    public Optional<String> inviteUrl() {
        if (!discord.hasBotToken()) {
            return Optional.empty();
        }
        return guildSettings
                .findFirstBy()
                .flatMap(settings -> settings.getInviteCode().or(() -> createInvite(settings)))
                .map(code -> INVITE_URL + code);
    }

    private Optional<String> createInvite(DiscordGuildSettings settings) {
        Optional<String> channelId = settings.getAnnouncementsChannelId();
        if (channelId.isEmpty()) {
            return Optional.empty();
        }
        try {
            Invite invite = restClient
                    .post()
                    .uri(INVITES_URI, channelId.get())
                    .header(AUTHORIZATION, "Bot " + discord.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("max_age", 0, "max_uses", 0))
                    .retrieve()
                    .body(Invite.class);
            if (invite == null || invite.code() == null) {
                throw new RestClientException("Discord created an invite without saying its code");
            }
            settings.setInviteCode(invite.code());
            guildSettings.save(settings);
            return Optional.of(invite.code());
        } catch (RestClientException ex) {
            log.warn("Discord would not let us create an invite to channel {}: {}", channelId.get(), ex.getMessage());
            return Optional.empty();
        }
    }

    private record Invite(String code) {}
}
