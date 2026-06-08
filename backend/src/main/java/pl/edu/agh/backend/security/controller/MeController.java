package pl.edu.agh.backend.security.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.security.config.SecurityConfig;
import pl.edu.agh.backend.security.controller.dto.MeResponse;

@RestController
@RequestMapping("/api/me")
public class MeController {

    @GetMapping
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            var claims = oidcUser.getClaims();
            return new MeResponse(
                    oidcUser.getPreferredUsername(),
                    oidcUser.getFullName(),
                    oidcUser.getEmail(),
                    extractRoles(claims));
        }

        return new MeResponse(authentication.getName(), null, null, List.of());
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(Map<String, Object> claims) {
        return SecurityConfig.extractRealmRoles(claims).stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
    }
}
