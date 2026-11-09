package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
class DiscordCommandRegistration {

    private static final String COMMANDS_URI = "https://discord.com/api/v10/applications/{applicationId}/commands";

    private final RestClient restClient;
    private final DiscordProperties discord;

    DiscordCommandRegistration(@Qualifier("discordRestClient") RestClient restClient, DiscordProperties discord) {
        this.restClient = restClient;
        this.discord = discord;
    }

    @EventListener(ApplicationReadyEvent.class)
    void register() {
        if (!discord.hasBotToken() || !discord.hasApplicationId()) {
            log.debug(
                    "Discord bot token or application id is not configured, /{} is not registered",
                    DiscordSetupCommand.NAME);
            return;
        }
        try {
            restClient
                    .put()
                    .uri(COMMANDS_URI, discord.applicationId())
                    .header(AUTHORIZATION, "Bot " + discord.botToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(DiscordSetupCommand.DEFINITION))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Registered /{} with Discord", DiscordSetupCommand.NAME);
        } catch (RestClientException ex) {
            log.warn("Discord would not register /{}: {}", DiscordSetupCommand.NAME, ex.getMessage());
        }
    }
}
