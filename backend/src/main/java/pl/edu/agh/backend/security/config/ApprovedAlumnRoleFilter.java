package pl.edu.agh.backend.security.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;

@RequiredArgsConstructor
class ApprovedAlumnRoleFilter extends OncePerRequestFilter {

    private static final GrantedAuthority VERIFIED_ALUMN =
            new SimpleGrantedAuthority(Roles.ROLE_PREFIX + Roles.VERIFIED_ALUMN);

    private final ApplicationRepository applicationRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Caller caller = Caller.from(authentication);
        if (!caller.isAnonymous()
                && !caller.hasRole(Roles.VERIFIED_ALUMN)
                && applicationRepository.existsByApplicantKeycloakIdAndStatus(
                        caller.keycloakId(), ApplicationStatus.APPROVED)) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(withVerifiedAlumn(authentication));
            SecurityContextHolder.setContext(context);
        }
        chain.doFilter(request, response);
    }

    private static Authentication withVerifiedAlumn(Authentication authentication) {
        List<GrantedAuthority> authorities = new ArrayList<>(authentication.getAuthorities());
        authorities.add(VERIFIED_ALUMN);
        return switch (authentication) {
            case JwtAuthenticationToken t -> new JwtAuthenticationToken(t.getToken(), authorities, t.getName());
            case OAuth2AuthenticationToken t ->
                new OAuth2AuthenticationToken(t.getPrincipal(), authorities, t.getAuthorizedClientRegistrationId());
            default -> authentication;
        };
    }
}
