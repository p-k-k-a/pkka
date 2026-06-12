package pl.edu.agh.backend.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
@Tag(name = "Me", description = "Current user identity and roles")
public class MeController {

    @GetMapping
    @Operation(summary = "Current user info", description = """
                    Returns identity claims and realm roles for the currently authenticated user.
                    Works for both web (OIDC session) and mobile (Bearer JWT) flows.
                    Roles are returned without the ROLE_ prefix.
                    """)
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return switch (authentication) {
            case OAuth2AuthenticationToken t
            when t.getPrincipal() instanceof OidcUser u ->
                new MeResponse(
                        u.getSubject(),
                        u.getEmail(),
                        u.getGivenName(),
                        u.getFamilyName(),
                        u.getPreferredUsername(),
                        extractRoles(authentication));

            case JwtAuthenticationToken t -> {
                var jwt = t.getToken();
                yield new MeResponse(
                        jwt.getSubject(),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("given_name"),
                        jwt.getClaimAsString("family_name"),
                        jwt.getClaimAsString("preferred_username"),
                        extractRoles(authentication));
            }

            default -> throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        };
    }

    private static List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
    }
}
