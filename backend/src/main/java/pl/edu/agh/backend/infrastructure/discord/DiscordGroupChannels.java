package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class DiscordGroupChannels {

    private static final String ROLES_URI = "https://discord.com/api/v10/guilds/{guildId}/roles";
    private static final String CHANNELS_URI = "https://discord.com/api/v10/guilds/{guildId}/channels";
    private static final String PERMISSION_URI =
            "https://discord.com/api/v10/channels/{channelId}/permissions/{targetId}";

    private static final int NAME_LIMIT = 100;
    private static final int TEXT_CHANNEL = 0;
    private static final int ROLE_TARGET = 0;
    private static final int MEMBER_TARGET = 1;
    private static final String NO_PERMISSIONS = "0";
    private static final String VIEW_CHANNEL = "1024";
    private static final String MANAGE_MESSAGES = "8192";

    private final RestClient restClient;
    private final DiscordProperties discord;
    private final DiscordGuildSettingsRepository guildSettings;

    public DiscordGroupChannels(
            @Qualifier("discordRestClient") RestClient restClient,
            DiscordProperties discord,
            DiscordGuildSettingsRepository guildSettings) {
        this.restClient = restClient;
        this.discord = discord;
        this.guildSettings = guildSettings;
    }

    public boolean isConfigured() {
        return configuredGuild().isPresent();
    }

    public Optional<String> createRole(String name) {
        return send(
                "create role " + name,
                guild -> id(restClient
                        .post()
                        .uri(ROLES_URI, guild.id())
                        .header(AUTHORIZATION, "Bot " + discord.botToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("name", truncate(name), "permissions", NO_PERMISSIONS, "mentionable", false))
                        .retrieve()
                        .body(Created.class)));
    }

    public Optional<String> createChannel(String name, String roleId) {
        return send(
                "create channel " + name,
                guild -> id(restClient
                        .post()
                        .uri(CHANNELS_URI, guild.id())
                        .header(AUTHORIZATION, "Bot " + discord.botToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "name",
                                truncate(name),
                                "type",
                                TEXT_CHANNEL,
                                "parent_id",
                                guild.categoryId(),
                                "permission_overwrites",
                                List.of(
                                        Map.of("id", guild.id(), "type", ROLE_TARGET, "deny", VIEW_CHANNEL),
                                        Map.of("id", roleId, "type", ROLE_TARGET, "allow", VIEW_CHANNEL),
                                        Map.of(
                                                "id",
                                                discord.applicationId(),
                                                "type",
                                                MEMBER_TARGET,
                                                "allow",
                                                VIEW_CHANNEL))))
                        .retrieve()
                        .body(Created.class)));
    }

    public boolean allowManageMessages(String channelId, String discordUserId) {
        return send(
                        "let " + discordUserId + " manage messages in channel " + channelId,
                        guild -> restClient
                                .put()
                                .uri(PERMISSION_URI, channelId, discordUserId)
                                .header(AUTHORIZATION, "Bot " + discord.botToken())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(Map.of("type", MEMBER_TARGET, "allow", MANAGE_MESSAGES))
                                .retrieve()
                                .toBodilessEntity())
                .isPresent();
    }

    private <T> Optional<T> send(String action, Function<Guild, T> call) {
        Optional<Guild> guild = configuredGuild();
        if (guild.isEmpty()) {
            log.warn("Discord is not set up for topic groups, cannot {}", action);
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(call.apply(guild.get()));
        } catch (RestClientException ex) {
            log.warn("Discord would not let us {}: {}", action, ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Guild> configuredGuild() {
        if (!discord.hasBotToken() || !discord.hasApplicationId()) {
            return Optional.empty();
        }
        return guildSettings
                .findFirstBy()
                .flatMap(settings ->
                        settings.getGroupsCategoryId().map(categoryId -> new Guild(settings.getGuildId(), categoryId)));
    }

    private static String id(Created created) {
        if (created == null || created.id() == null) {
            throw new RestClientException("Discord created something without saying its id");
        }
        return created.id();
    }

    private static String truncate(String name) {
        return name.length() <= NAME_LIMIT ? name : name.substring(0, NAME_LIMIT);
    }

    private record Guild(String id, String categoryId) {}

    private record Created(String id) {}
}
