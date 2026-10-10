package pl.edu.agh.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import jakarta.servlet.http.Cookie;
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

    @Test
    void logoutClearsCsrfCookieOnConfiguredDomainAndSessionCookieOnApiHost() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new WebCookieDomainConfig("pkka.me")
                .webCookieClearingLogoutHandler()
                .logout(new MockHttpServletRequest(), response, null);

        assertThat(response.getCookies())
                .extracting(Cookie::getName, Cookie::getDomain, Cookie::getMaxAge, Cookie::getPath)
                .containsExactlyInAnyOrder(tuple("JSESSIONID", null, 0, "/"), tuple("XSRF-TOKEN", "pkka.me", 0, "/"));
    }

    @Test
    void logoutClearsHostOnlyCookiesWhenNoDomainIsConfigured() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new WebCookieDomainConfig("")
                .webCookieClearingLogoutHandler()
                .logout(new MockHttpServletRequest(), response, null);

        assertThat(response.getCookies())
                .extracting(Cookie::getName, Cookie::getDomain, Cookie::getMaxAge)
                .containsExactlyInAnyOrder(tuple("JSESSIONID", null, 0), tuple("XSRF-TOKEN", null, 0));
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
