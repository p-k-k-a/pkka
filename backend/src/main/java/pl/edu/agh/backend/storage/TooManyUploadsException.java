package pl.edu.agh.backend.storage;

import java.time.Duration;

/** The caller has asked for too many upload URLs. Answered as 429 with {@code Retry-After}. */
public class TooManyUploadsException extends RuntimeException {

    private final transient Duration retryAfter;

    public TooManyUploadsException(Duration retryAfter) {
        super("Too many uploads, try again later");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
