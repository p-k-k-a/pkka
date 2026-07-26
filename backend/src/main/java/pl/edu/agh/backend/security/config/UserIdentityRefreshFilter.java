package pl.edu.agh.backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.UserProvisioningService;

@RequiredArgsConstructor
@Slf4j
public class UserIdentityRefreshFilter extends OncePerRequestFilter {

    private final UserPrincipalExtractor principalExtractor;
    private final UserProvisioningService userProvisioningService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            principalExtractor.extract(authentication).ifPresent(this::refreshQuietly);
        }
        filterChain.doFilter(request, response);
    }

    private void refreshQuietly(UserPrincipalExtractor.UserPrincipalInfo info) {
        try {
            userProvisioningService.refreshIdentityIfStale(info.keycloakId());
        } catch (RuntimeException ex) {
            log.warn("Identity refresh failed for user {}: {}", info.keycloakId(), ex.getMessage());
        }
    }
}
