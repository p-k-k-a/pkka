package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class DiscordGuildMembership {

    private static final String GUILD_MEMBER_URI = "https://discord.com/api/v10/guilds/{guildId}/members/{userId}";
    private static final String MEMBER_ROLE_URI =
            "https://discord.com/api/v10/guilds/{guildId}/members/{userId}/roles/{roleId}";
    private static final Duration DEFAULT_RETRY_AFTER = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final DiscordProperties discord;
    private final DiscordGuildSettingsRepository guildSettings;

    public DiscordGuildMembership(
            @Qualifier("discordRestClient") RestClient restClient,
            DiscordProperties discord,
            DiscordGuildSettingsRepository guildSettings) {
        this.restClient = restClient;
        this.discord = discord;
        this.guildSettings = guildSettings;
    }

    public boolean isConfigured() {
        return discord.hasBotToken() && guildSettings.findFirstBy().isPresent();
    }

    public Optional<String> alumnRoleId() {
        return guildSettings.findFirstBy().flatMap(DiscordGuildSettings::getAlumnRoleId);
    }

    /** The member's roles, or empty when they are not on the server. */
    public Optional<List<String>> roles(String discordId) {
        return guildRoles(guildId(), discordId);
    }

    public void addRole(String discordId, String roleId) {
        changeRole(HttpMethod.PUT, discordId, roleId);
    }

    public void removeRole(String discordId, String roleId) {
        changeRole(HttpMethod.DELETE, discordId, roleId);
    }

    public boolean holdsAlumnRole(String discordId) {
        if (!discord.hasBotToken()) {
            log.debug("Discord bot token is not configured, vouching for nobody");
            return false;
        }
        Optional<DiscordGuildSettings> guild = guildSettings.findFirstBy();
        Optional<String> alumnRoleId = guild.flatMap(DiscordGuildSettings::getAlumnRoleId);
        if (guild.isEmpty() || alumnRoleId.isEmpty()) {
            log.debug("No Discord guild or alumn role is set, vouching for nobody");
            return false;
        }
        try {
            return guildRoles(guild.get().getGuildId(), discordId)
                    .orElse(List.of())
                    .contains(alumnRoleId.get());
        } catch (RestClientException ex) {
            log.warn("Discord could not be asked about {}, vouching for nobody: {}", discordId, ex.getMessage());
            return false;
        }
    }

    private Optional<List<String>> guildRoles(String guildId, String discordId) {
        return restClient
                .get()
                .uri(GUILD_MEMBER_URI, guildId, discordId)
                .header(AUTHORIZATION, "Bot " + discord.botToken())
                .exchange((request, response) -> switch (response.getStatusCode()
                        .value()) {
                    case 200 -> Optional.of(response.bodyTo(GuildMember.class).roles());
                    case 404 -> Optional.<List<String>>empty();
                    case 429 -> throw rateLimited(response);
                    default -> throw new RestClientException("Discord returned " + response.getStatusCode());
                });
    }

    private void changeRole(HttpMethod method, String discordId, String roleId) {
        restClient
                .method(method)
                .uri(MEMBER_ROLE_URI, guildId(), discordId, roleId)
                .header(AUTHORIZATION, "Bot " + discord.botToken())
                .retrieve()
                .onStatus(status -> status.value() == 429, (request, response) -> {
                    throw rateLimited(response);
                })
                .toBodilessEntity();
    }

    private String guildId() {
        return guildSettings
                .findFirstBy()
                .map(DiscordGuildSettings::getGuildId)
                .orElseThrow(() -> new RestClientException("Discord is not set up"));
    }

    private static DiscordRateLimitedException rateLimited(ClientHttpResponse response) {
        String seconds = response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        return new DiscordRateLimitedException(
                seconds == null ? DEFAULT_RETRY_AFTER : Duration.ofMillis((long) (Double.parseDouble(seconds) * 1000)));
    }

    private record GuildMember(List<String> roles) {}
}
