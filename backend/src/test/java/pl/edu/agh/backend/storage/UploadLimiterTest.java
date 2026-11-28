package pl.edu.agh.backend.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.edu.agh.backend.storage.UploadLimiter.Kind;

class UploadLimiterTest {

    private static final Instant START = Instant.parse("2026-11-24T12:00:00Z");

    private final Clock clock = mock(Clock.class);
    private final UploadLimiter limiter = new UploadLimiter(clock);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(START);
    }

    @Test
    void refusesOnceTheLimitIsUsedUpAndSaysHowLongToWait() {
        useUp("kc-1");

        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(23)));
        assertThatThrownBy(() -> limiter.count(Kind.AVATAR, "kc-1"))
                .isInstanceOfSatisfying(
                        TooManyUploadsException.class,
                        ex -> assertThat(ex.retryAfter()).isEqualTo(Duration.ofHours(1)));
    }

    @Test
    void startsOverAfterTheWindowEvenWhenRefusedInTheMeantime() {
        useUp("kc-1");
        when(clock.instant()).thenReturn(START.plus(Duration.ofHours(20)));
        assertThatThrownBy(() -> limiter.count(Kind.AVATAR, "kc-1")).isInstanceOf(TooManyUploadsException.class);

        when(clock.instant()).thenReturn(START.plus(UploadLimiter.WINDOW));
        assertThatCode(() -> useUp("kc-1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.count(Kind.AVATAR, "kc-1")).isInstanceOf(TooManyUploadsException.class);
    }

    @Test
    void countsPeopleAndKindsSeparately() {
        useUp("kc-1");

        assertThatCode(() -> limiter.count(Kind.AVATAR, "kc-2")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.count(Kind.ADMIN, "kc-1")).doesNotThrowAnyException();
    }

    private void useUp(String person) {
        for (int i = 0; i < Kind.AVATAR.maxPerWindow(); i++) {
            limiter.count(Kind.AVATAR, person);
        }
    }
}
