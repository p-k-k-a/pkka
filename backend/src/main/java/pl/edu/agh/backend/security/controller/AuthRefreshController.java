package pl.edu.agh.backend.security.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/auth")
public class AuthRefreshController {
    private final ClientRegistration mobile;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthRefreshController(ClientRegistrationRepository repo) {
        this.mobile = repo.findByRegistrationId("keycloak-mobile");
    }

    // These probably should be in some DTO folder, will have to be changed later
    public record RefreshRequest(String refreshToken) {}

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken) {}

    private static String requireRefreshToken(RefreshRequest body) {
        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing refresh token");
        }
        return body.refreshToken();
    }

    private String extractOAuthError(ClientHttpResponse response) {
        try {
            return objectMapper.readTree(response.getBody()).path("error").asText(null);
        } catch (IOException e) {
            return null;
        }
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest body) {
        MultiValueMap<String, String> form = MultiValueMap.fromSingleValue(Map.of(
                "grant_type", "refresh_token",
                "refresh_token", requireRefreshToken(body),
                "client_id", mobile.getClientId(),
                "client_secret", mobile.getClientSecret()));

        return restClient
                .post()
                .uri(mobile.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.isError(), (req, res) -> {
                    // Keycloak returns 400 invalid_grant when the refresh token is expired/revoked
                    // (RFC 6749 §5.2) -> the user must re-authenticate. Anything else (e.g.
                    // invalid_client from a misconfigured client secret) is our problem, not the
                    // user's, so surface it as a 502 instead of telling them to log in again.
                    if ("invalid_grant".equals(extractOAuthError(res))) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Token refresh failed upstream");
                })
                .body(TokenResponse.class);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest body) {
        MultiValueMap<String, String> form = MultiValueMap.fromSingleValue(Map.of(
                "client_id", mobile.getClientId(),
                "client_secret", mobile.getClientSecret(),
                "refresh_token", requireRefreshToken(body)));

        restClient
                .post()
                .uri(mobile.getProviderDetails()
                        .getConfigurationMetadata()
                        .get("end_session_endpoint")
                        .toString())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                .toBodilessEntity();
    }
}
