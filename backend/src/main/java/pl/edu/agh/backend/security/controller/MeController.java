package pl.edu.agh.backend.security.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return new MeResponse(
                    oidcUser.getPreferredUsername(),
                    oidcUser.getFullName(),
                    oidcUser.getEmail(),
                    extractRoles(authentication));
        }

        return new MeResponse(authentication.getName(), null, null, extractRoles(authentication));
    }

    private static List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
    }
}
