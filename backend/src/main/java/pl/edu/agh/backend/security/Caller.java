package pl.edu.agh.backend.security;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whoever made the current request, reduced to what the application actually needs: who they are, what
 * they may do and which app they came through. Roles come without the {@code ROLE_} prefix, matching the
 * constants in {@link Roles}.
 */
public record Caller(String keycloakId, Set<String> roles, Client client) {

    private static final Caller ANONYMOUS = new Caller(null, Set.of());

    /** The app the request came from; absent for anonymous callers. */
    public enum Client {
        WEB,
        MOBILE
    }

    public Caller {
        roles = Set.copyOf(roles);
    }

    /** For callers whose app does not matter, typically built by hand in tests. */
    public Caller(String keycloakId, Set<String> roles) {
        this(keycloakId, roles, null);
    }

    public static Caller anonymous() {
        return ANONYMOUS;
    }

    public static Caller from(Authentication authentication) {
        return keycloakId(authentication)
                .map(id -> new Caller(id, roles(authentication), client(authentication)))
                .orElse(ANONYMOUS);
    }

    public boolean isAnonymous() {
        return keycloakId == null;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public String requireKeycloakId() {
        if (isAnonymous()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return keycloakId;
    }

    private static Optional<String> keycloakId(Authentication authentication) {
        return switch (authentication) {
            case OAuth2AuthenticationToken t when t.getPrincipal() instanceof OidcUser u -> Optional.of(u.getSubject());
            case JwtAuthenticationToken t -> Optional.of(t.getToken().getSubject());
            case null, default -> Optional.empty();
        };
    }

    /** The web app signs in through a server-side session, the mobile app with a bearer token. */
    private static Client client(Authentication authentication) {
        return switch (authentication) {
            case OAuth2AuthenticationToken t -> Client.WEB;
            case JwtAuthenticationToken t -> Client.MOBILE;
            case null, default -> null;
        };
    }

    private static Set<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith(Roles.ROLE_PREFIX))
                .map(authority -> authority.substring(Roles.ROLE_PREFIX.length()))
                .collect(Collectors.toUnmodifiableSet());
    }
}
