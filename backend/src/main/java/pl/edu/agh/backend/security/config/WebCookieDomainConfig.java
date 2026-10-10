package pl.edu.agh.backend.security.config;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

// The web app (pkka.me) reads the CSRF cookie in the browser and copies it into a header for the
// API (api.pkka.me), so in production that cookie is scoped to the shared parent domain. Left
// empty (local dev, where cookies ignore the port), it stays host-only.
@Configuration
public class WebCookieDomainConfig {

    private static final String SESSION_COOKIE = "JSESSIONID";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";

    private final String domain;

    public WebCookieDomainConfig(@Value("${app.web.cookie-domain:}") String domain) {
        this.domain = domain.trim();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (!domain.isEmpty()) {
            repository.setCookieCustomizer(cookie -> cookie.domain(domain));
        }
        return repository;
    }

    // Logout deletes a cookie by sending it again, already expired. The browser only replaces the
    // stored cookie if the domain matches, so XSRF-TOKEN is expired with the configured domain and
    // JSESSIONID, which never has one, without a domain.
    @Bean
    public LogoutHandler webCookieClearingLogoutHandler() {
        return new CookieClearingLogoutHandler(expired(SESSION_COOKIE, ""), expired(CSRF_COOKIE, domain));
    }

    private static Cookie expired(String name, String domain) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        if (!domain.isEmpty()) {
            cookie.setDomain(domain);
        }
        return cookie;
    }
}
