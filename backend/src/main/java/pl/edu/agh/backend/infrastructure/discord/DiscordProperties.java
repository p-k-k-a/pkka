package pl.edu.agh.backend.infrastructure.discord;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord")
public record DiscordProperties(String guildId, String botToken, List<String> acceptedRoleIds) {

    public DiscordProperties {
        acceptedRoleIds = acceptedRoleIds == null ? List.of() : List.copyOf(acceptedRoleIds);
    }

    boolean isConfigured() {
        return guildId != null && !guildId.isBlank() && botToken != null && !botToken.isBlank();
    }
}
