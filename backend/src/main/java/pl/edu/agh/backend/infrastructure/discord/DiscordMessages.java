package pl.edu.agh.backend.infrastructure.discord;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class DiscordMessages {

    private static final String CHANNEL_MESSAGES_URI = "https://discord.com/api/v10/channels/{channelId}/messages";

    private final RestClient restClient;
    private final DiscordProperties discord;

    public DiscordMessages(@Qualifier("discordRestClient") RestClient restClient, DiscordProperties discord) {
        this.restClient = restClient;
        this.discord = discord;
    }

    public Optional<String> post(String channelId, DiscordEmbed embed) {
        // Retried under the same nonce, Discord returns the message it already created instead of a duplicate.
        Map<String, Object> message = Map.of("embeds", List.of(embed), "nonce", nonce(), "enforce_nonce", true);
        Supplier<String> attempt = () -> restClient
                .post()
                .uri(CHANNEL_MESSAGES_URI, channelId)
                .header(AUTHORIZATION, "Bot " + discord.botToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(message)
                .exchange(
                        (request, response) -> switch (response.getStatusCode().value()) {
                            case 200 -> messageId(response.bodyTo(PostedMessage.class));
                            default -> throw refused(response.getStatusCode());
                        });
        return send("post to channel " + channelId, () -> {
            try {
                return attempt.get();
            } catch (ResourceAccessException ex) {
                log.info("Discord did not answer a post to channel {} in time, asking again", channelId);
                return attempt.get();
            }
        });
    }

    private <T> Optional<T> send(String action, Supplier<T> call) {
        if (!discord.hasBotToken()) {
            log.debug("Discord bot token is not configured, cannot {}", action);
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(call.get());
        } catch (RestClientException ex) {
            log.warn("Discord would not let us {}: {}", action, ex.getMessage());
            return Optional.empty();
        }
    }

    private static String nonce() {
        return Long.toString(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE), Character.MAX_RADIX);
    }

    private static String messageId(PostedMessage posted) {
        if (posted == null || posted.id() == null) {
            throw new RestClientException("Discord accepted a message without saying its id");
        }
        return posted.id();
    }

    private static RestClientException refused(HttpStatusCode status) {
        return new RestClientException("Discord returned " + status);
    }

    private record PostedMessage(String id) {}
}
