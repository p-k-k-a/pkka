package pl.edu.agh.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;

class WebCookieDomainConfigTest {

    @Test
    void csrfCookieIsScopedToConfiguredDomain() {
        String setCookie = savedCsrfCookie(new WebCookieDomainConfig("pkka.me"));

        assertThat(setCookie).startsWith("XSRF-TOKEN=").contains("Domain=pkka.me");
    }

    @Test
    void csrfCookieIsHostOnlyWhenNoDomainIsConfigured() {
        String setCookie = savedCsrfCookie(new WebCookieDomainConfig(""));

        assertThat(setCookie).startsWith("XSRF-TOKEN=").doesNotContain("Domain=");
    }

    @Test
    void csrfCookieStaysReadableByBrowserScripts() {
        String setCookie = savedCsrfCookie(new WebCookieDomainConfig("pkka.me"));

        assertThat(setCookie).doesNotContain("HttpOnly");
    }

    private static String savedCsrfCookie(WebCookieDomainConfig config) {
        CookieCsrfTokenRepository repository = config.csrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = repository.generateToken(request);

        repository.saveToken(token, request, response);

        return response.getHeader("Set-Cookie");
    }
}
