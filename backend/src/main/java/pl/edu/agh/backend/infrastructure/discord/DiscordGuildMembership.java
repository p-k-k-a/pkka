package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class DiscordGuildMembership {

    private static final String GUILD_MEMBER_URI = "https://discord.com/api/v10/guilds/{guildId}/members/{userId}";

    private final RestClient restClient;
    private final DiscordProperties discord;

    public DiscordGuildMembership(@Qualifier("discordRestClient") RestClient restClient, DiscordProperties discord) {
        this.restClient = restClient;
        this.discord = discord;
    }

    public boolean holdsAcceptedRole(String discordId) {
        if (!discord.isConfigured()) {
            log.debug("Discord guild or bot token is not configured, vouching for nobody");
            return false;
        }
        try {
            return guildRoles(discordId).stream().anyMatch(discord.acceptedRoleIds()::contains);
        } catch (RestClientException ex) {
            log.warn("Discord could not be asked about {}, vouching for nobody: {}", discordId, ex.getMessage());
            return false;
        }
    }

    private List<String> guildRoles(String discordId) {
        return restClient
                .get()
                .uri(GUILD_MEMBER_URI, discord.guildId(), discordId)
                .header(AUTHORIZATION, "Bot " + discord.botToken())
                .exchange(
                        (request, response) -> switch (response.getStatusCode().value()) {
                            case 200 -> response.bodyTo(GuildMember.class).roles();
                            case 404 -> List.<String>of();
                            default -> throw new RestClientException("Discord returned " + response.getStatusCode());
                        });
    }

    private record GuildMember(List<String> roles) {}
}
