package pl.edu.agh.backend.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Limits how many upload URLs one person gets within 24 hours, so nobody can fill the bucket faster than
 * {@link StorageCleanup} empties it. The 24 hours start with the first URL.
 *
 * <p>Counted in memory: a restart starts the count over, and several backend instances would each count on
 * their own. Good enough for capping abuse, which is all this is for.
 */
@Component
public class UploadLimiter {

    /** What is counted separately, with how many URLs of it one person gets. */
    public enum Kind {
        AVATAR(20),
        /** Files of club content. Admins are trusted; this only stops a runaway script or a stolen session. */
        ADMIN(300);

        private final int maxPerWindow;

        Kind(int maxPerWindow) {
            this.maxPerWindow = maxPerWindow;
        }

        int maxPerWindow() {
            return maxPerWindow;
        }
    }

    static final Duration WINDOW = Duration.ofHours(24);

    private record Key(Kind kind, String person) {}

    private record Usage(Instant since, int count) {}

    private final Clock clock;

    /** Forgets a person a window after their last URL, by when their count no longer matters. */
    private final Cache<Key, Usage> usages;

    public UploadLimiter(Clock clock) {
        this.clock = clock;
        this.usages = Caffeine.newBuilder().expireAfterWrite(WINDOW).build();
    }

    /**
     * Counts one upload URL for {@code person}.
     *
     * @throws TooManyUploadsException when they have used up their limit; this attempt is then not counted
     */
    public void count(Kind kind, String person) {
        Instant now = clock.instant();
        usages.asMap().compute(new Key(kind, person), (key, usage) -> {
            if (usage == null || !now.isBefore(usage.since().plus(WINDOW))) {
                return new Usage(now, 1);
            }
            if (usage.count() >= kind.maxPerWindow()) {
                throw new TooManyUploadsException(
                        Duration.between(now, usage.since().plus(WINDOW)));
            }
            return new Usage(usage.since(), usage.count() + 1);
        });
    }
}
