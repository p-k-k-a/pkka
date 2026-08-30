package pl.edu.agh.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SpaCsrfTokenRequestHandlerTest {

    private final SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();

    @Test
    void resolvesPlainHeaderValueWhenHeaderPresent() {
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "plain-token");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-XSRF-TOKEN", "plain-token");

        assertThat(handler.resolveCsrfTokenValue(request, token)).isEqualTo("plain-token");
    }
}
