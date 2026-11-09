package pl.edu.agh.backend.infrastructure.discord;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("discord")
public record DiscordProperties(String botToken, String applicationId, String publicKey) {

    boolean hasBotToken() {
        return botToken != null && !botToken.isBlank();
    }

    boolean hasApplicationId() {
        return applicationId != null && !applicationId.isBlank();
    }
}
