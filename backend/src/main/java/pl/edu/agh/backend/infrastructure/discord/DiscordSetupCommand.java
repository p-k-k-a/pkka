package pl.edu.agh.backend.infrastructure.discord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
@Slf4j
class DiscordSetupCommand {

    static final String NAME = "konfiguracja";

    private static final int CHANNEL_OPTION = 7;
    private static final int ROLE_OPTION = 8;
    private static final int TEXT_CHANNEL = 0;
    private static final int ANNOUNCEMENT_CHANNEL = 5;
    private static final int IN_SERVERS_ONLY = 0;
    private static final int CHANNEL_MESSAGE_WITH_SOURCE = 4;
    private static final int EPHEMERAL = 64;

    static final Map<String, Object> DEFINITION = Map.of(
            "name",
            NAME,
            "description",
            "Kanał ogłoszeń wydarzeń i rola absolwenta",
            "default_member_permissions",
            "8",
            "contexts",
            List.of(IN_SERVERS_ONLY),
            "options",
            List.of(
                    Map.of(
                            "type",
                            CHANNEL_OPTION,
                            "name",
                            "kanal",
                            "description",
                            "Kanał, na którym bot ogłasza wydarzenia",
                            "channel_types",
                            List.of(TEXT_CHANNEL, ANNOUNCEMENT_CHANNEL)),
                    Map.of(
                            "type", ROLE_OPTION,
                            "name", "rola",
                            "description", "Rola, której posiadacze są absolwentami")));

    private static final DiscordEmbed CHANNEL_CONFIRMATION =
            new DiscordEmbed(null, "Tu będą pojawiać się ogłoszenia wydarzeń z platformy.", List.of());

    private static final String OTHER_SERVER = "Bot jest już skonfigurowany na innym serwerze.";
    private static final String NOT_A_PLAIN_ROLE = "Wybierz rolę nadawaną absolwentom, a nie @everyone ani rolę bota.";
    private static final String CHANNEL_PERMISSIONS =
            "Daj mu tam: Wyświetlanie kanału, Wysyłanie wiadomości i Osadzanie linków.";

    private final DiscordGuildSettingsRepository guildSettings;
    private final DiscordMessages messages;

    Map<String, Object> handle(JsonNode interaction) {
        String guildId = interaction.path("guild_id").asString();
        JsonNode data = interaction.path("data");
        Optional<String> channelId = option(data, "kanal");
        Optional<String> roleId = option(data, "rola");

        Optional<DiscordGuildSettings> current = guildSettings.findFirstBy();
        if (current.isPresent() && !current.get().getGuildId().equals(guildId)) {
            return reply(OTHER_SERVER);
        }
        if (channelId.isEmpty() && roleId.isEmpty()) {
            return reply(describe(current));
        }
        if (roleId.isPresent() && !isPlainRole(data, guildId, roleId.get())) {
            return reply(NOT_A_PLAIN_ROLE);
        }
        if (channelId.isPresent()
                && messages.post(channelId.get(), CHANNEL_CONFIRMATION).isEmpty()) {
            return reply("Bot nie może pisać na <#" + channelId.get() + ">. " + CHANNEL_PERMISSIONS);
        }

        DiscordGuildSettings settings = current.orElseGet(() -> new DiscordGuildSettings(guildId, null, null));
        settings.configure(channelId, roleId);
        try {
            settings = guildSettings.saveAndFlush(settings);
        } catch (DataIntegrityViolationException ex) {
            return reply(OTHER_SERVER);
        }
        log.info(
                "Discord setup changed on guild {} by {}: channel {}, alumn role {}",
                guildId,
                interaction.path("member").path("user").path("id").asString(),
                channelId.orElse("unchanged"),
                roleId.orElse("unchanged"));
        return reply(describe(Optional.of(settings)));
    }

    private static boolean isPlainRole(JsonNode data, String guildId, String roleId) {
        boolean managed =
                data.path("resolved").path("roles").path(roleId).path("managed").asBoolean();
        return !roleId.equals(guildId) && !managed;
    }

    private static Optional<String> option(JsonNode data, String name) {
        for (JsonNode option : data.path("options")) {
            if (name.equals(option.path("name").asString())) {
                return Optional.of(option.path("value").asString());
            }
        }
        return Optional.empty();
    }

    private static String describe(Optional<DiscordGuildSettings> settings) {
        String channel = settings.flatMap(DiscordGuildSettings::getAnnouncementsChannelId)
                .map(id -> "<#" + id + ">")
                .orElse("nie ustawiono");
        String role = settings.flatMap(DiscordGuildSettings::getAlumnRoleId)
                .map(id -> "<@&" + id + ">")
                .orElse("nie ustawiono");
        return "Kanał ogłoszeń: %s\nRola absolwenta: %s".formatted(channel, role);
    }

    private static Map<String, Object> reply(String content) {
        return Map.of(
                "type",
                CHANNEL_MESSAGE_WITH_SOURCE,
                "data",
                Map.of("content", content, "flags", EPHEMERAL, "allowed_mentions", Map.of("parse", List.of())));
    }
}
