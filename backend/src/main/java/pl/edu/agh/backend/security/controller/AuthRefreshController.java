package pl.edu.agh.backend.security.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/auth")
public class AuthRefreshController {
    private final ClientRegistration mobile;
    private final RestClient restClient = RestClient.create();

    public AuthRefreshController(ClientRegistrationRepository repo) {
        this.mobile = repo.findByRegistrationId("keycloak-mobile");
    }

    // These probably should be in some DTO folder, will have to be changed later
    public record RefreshRequest(String refreshToken) {}

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken) {}

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest body) {
        MultiValueMap<String, String> form =
                MultiValueMap.fromSingleValue(
                        Map.of(
                                "grant_type", "refresh_token",
                                "refresh_token", body.refreshToken(),
                                "client_id", mobile.getClientId(),
                                "client_secret", mobile.getClientSecret()));

        return restClient
                .post()
                .uri(mobile.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        (req, res) -> {
                            throw new ResponseStatusException(
                                    HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
                        })
                .body(TokenResponse.class);
    }
}
