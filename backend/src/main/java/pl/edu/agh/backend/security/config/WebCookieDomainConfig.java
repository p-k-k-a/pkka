package pl.edu.agh.backend.security.config;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

// The web app (pkka.me) reads the CSRF cookie in the browser and sends the session cookie to the
// API (api.pkka.me), so in production both cookies are scoped to the shared parent domain. Left
// empty (local dev, where cookies ignore the port), both stay host-only.
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

    @Bean
    public ServletContextInitializer sessionCookieDomainInitializer() {
        return servletContext -> {
            if (!domain.isEmpty()) {
                servletContext.getSessionCookieConfig().setDomain(domain);
            }
        };
    }

    // A browser only deletes a cookie when the expired copy has the same domain and path, so the
    // logout cookies must carry the configured domain too.
    @Bean
    public LogoutHandler webCookieClearingLogoutHandler() {
        return new CookieClearingLogoutHandler(expired(SESSION_COOKIE), expired(CSRF_COOKIE));
    }

    private Cookie expired(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        if (!domain.isEmpty()) {
            cookie.setDomain(domain);
        }
        return cookie;
    }
}
