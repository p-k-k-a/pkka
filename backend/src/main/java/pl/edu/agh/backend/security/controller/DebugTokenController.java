package pl.edu.agh.backend.security.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
public class DebugTokenController {
    private final OAuth2AuthorizedClientService clientService;

    public DebugTokenController(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/debug/token")
    public ResponseEntity<List<Map<String, Object>>> debugToken(
            OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2AuthorizedClient client =
                clientService.loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<Map<String, Object>> body = new ArrayList<>();

        OidcIdToken IDT = null;
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            IDT = oidcUser.getIdToken();
        }

        if (IDT != null) {
            Map<String, Object> IDTbody = new LinkedHashMap<>();
            IDTbody.put("tokenType", "id_token");
            IDTbody.put("idToken", IDT.getTokenValue());
            IDTbody.put("issuedAt", toStringOrNull(IDT.getIssuedAt()));
            IDTbody.put("expiresAt", toStringOrNull(IDT.getExpiresAt()));
            IDTbody.put("claims", IDT.getClaims());
            IDTbody.put("clientRegistrationId", authentication.getAuthorizedClientRegistrationId());
            IDTbody.put("principalName", authentication.getName());
            body.add(IDTbody);
        }

        OAuth2AccessToken AT = client.getAccessToken();
        Map<String, Object> ATbody = new LinkedHashMap<>();
        ATbody.put("tokenType", AT.getTokenType().getValue());
        ATbody.put("accessToken", AT.getTokenValue());
        ATbody.put("issuedAt", toStringOrNull(AT.getIssuedAt()));
        ATbody.put("expiresAt", toStringOrNull(AT.getExpiresAt()));
        ATbody.put("scopes", AT.getScopes());
        ATbody.put("clientRegistrationId", authentication.getAuthorizedClientRegistrationId());
        ATbody.put("principalName", authentication.getName());
        body.add(ATbody);

        OAuth2RefreshToken RT = client.getRefreshToken();
        if (RT != null) {
            Map<String, Object> RTbody = new LinkedHashMap<>();
            RTbody.put("refreshToken", RT.getTokenValue());
            RTbody.put("issuedAt", toStringOrNull(RT.getIssuedAt()));
            RTbody.put("expiresAt", toStringOrNull(RT.getExpiresAt()));
            RTbody.put("clientRegistrationId", authentication.getAuthorizedClientRegistrationId());
            RTbody.put("principalName", authentication.getName());
            body.add(RTbody);
        }

        return ResponseEntity.ok(body);
    }

    private String toStringOrNull(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
