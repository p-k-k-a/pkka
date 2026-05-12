package pl.edu.agh.backend.security;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
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
    public ResponseEntity<Map<String, Object>> debugToken(OAuth2AuthenticationToken authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(), authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        OAuth2AccessToken token = client.getAccessToken();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tokenType", token.getTokenType().getValue());
        body.put("accessToken", token.getTokenValue());
        body.put("issuedAt", toStringOrNull(token.getIssuedAt()));
        body.put("expiresAt", toStringOrNull(token.getExpiresAt()));
        body.put("scopes", token.getScopes());
        body.put("clientRegistrationId", authentication.getAuthorizedClientRegistrationId());
        body.put("principalName", authentication.getName());
        return ResponseEntity.ok(body);
    }

    private String toStringOrNull(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
