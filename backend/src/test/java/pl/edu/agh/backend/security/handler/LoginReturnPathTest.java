package pl.edu.agh.backend.security.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LoginReturnPathTest {

    @ParameterizedTest
    @ValueSource(strings = {"/events/zjazd-2026", "/dashboard/events/zjazd?tab=info", "/", "/events/a%C5%BC#top"})
    void acceptsPathsOnTheWebOrigin(String path) {
        assertThat(LoginReturnPath.sanitize(path)).contains(path);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "//evil.example",
                "/\\evil.example",
                "https://evil.example/events",
                "events/relative",
                "javascript:alert(1)",
                "/events/a b",
                "/events/\nset-cookie",
                "/events/%zz",
                "/events/<script>"
            })
    void rejectsAnythingThatCouldLeaveTheOrigin(String path) {
        assertThat(LoginReturnPath.sanitize(path)).isEmpty();
    }

    @Test
    void rejectsOverlongPaths() {
        assertThat(LoginReturnPath.sanitize("/" + "a".repeat(600))).isEmpty();
    }
}
