package pl.edu.agh.backend.security.controller;

import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.security.controller.dto.MeResponse;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return switch (authentication) {
            case OAuth2AuthenticationToken t
            when t.getPrincipal() instanceof OidcUser u ->
                new MeResponse(u.getPreferredUsername(), u.getFullName(), u.getEmail(), extractRoles(authentication));

            case JwtAuthenticationToken t -> {
                var jwt = t.getToken();
                yield new MeResponse(
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getClaimAsString("name"),
                        jwt.getClaimAsString("email"),
                        extractRoles(authentication));
            }

            default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        };
    }

    private static List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
    }
}
