package pl.edu.agh.backend.user;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserPrincipalExtractor {

    public Optional<UserPrincipalInfo> extract(Authentication authentication) {
        return switch (authentication) {
            case OAuth2AuthenticationToken t
            when t.getPrincipal() instanceof OidcUser u -> Optional.of(new UserPrincipalInfo(u.getSubject()));
            case JwtAuthenticationToken t -> {
                var jwt = t.getToken();
                yield Optional.of(new UserPrincipalInfo(jwt.getSubject()));
            }
            default -> Optional.empty();
        };
    }

    public record UserPrincipalInfo(String keycloakId) {}
}
