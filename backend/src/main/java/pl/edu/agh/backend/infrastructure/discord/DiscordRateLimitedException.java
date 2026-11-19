package pl.edu.agh.backend.infrastructure.discord;

import java.time.Duration;
import org.springframework.web.client.RestClientException;

public class DiscordRateLimitedException extends RestClientException {

    private final Duration retryAfter;

    public DiscordRateLimitedException(Duration retryAfter) {
        super("Discord asked us to wait " + retryAfter);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
