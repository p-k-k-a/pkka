package pl.edu.agh.backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Eagerly renders the deferred {@link CsrfToken} on every request so that the {@code XSRF-TOKEN} cookie is written to
 * the response. Without this, Spring Security 6 keeps the token deferred and the SPA never receives the cookie it needs
 * to echo back on state-changing requests.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Accessing the token value triggers CookieCsrfTokenRepository to add the Set-Cookie header.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
