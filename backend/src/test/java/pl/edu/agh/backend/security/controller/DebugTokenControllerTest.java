package pl.edu.agh.backend.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

class DebugTokenControllerTest {

    @Test
    void debugTokenReturnsIdAccessAndRefreshTokens() {
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        DebugTokenController controller = new DebugTokenController(clientService);

        OidcIdToken idToken = new OidcIdToken(
                "id-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "kc-1", "email", "dev@example.com"));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var oidcUser = new DefaultOidcUser(authorities, idToken);
        var auth = new OAuth2AuthenticationToken(oidcUser, authorities, "keycloak-web");

        OAuth2AuthorizedClient client = new OAuth2AuthorizedClient(
                ClientRegistration.withRegistrationId("keycloak-web")
                        .clientId("pkka-web")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("http://localhost/auth")
                        .tokenUri("http://localhost/token")
                        .issuerUri("http://localhost/realms/pkka")
                        .build(),
                auth.getName(),
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token-value",
                        Instant.now(),
                        Instant.now().plusSeconds(3600)),
                new OAuth2RefreshToken("refresh-token-value", Instant.now()));

        when(clientService.loadAuthorizedClient("keycloak-web", auth.getName())).thenReturn(client);

        ResponseEntity<List<Map<String, Object>>> response = controller.debugToken(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).get("tokenType")).isEqualTo("id_token");
        assertThat(response.getBody().get(1).get("accessToken")).isEqualTo("access-token-value");
        assertThat(response.getBody().get(2).get("refreshToken")).isEqualTo("refresh-token-value");
    }

    @Test
    void debugTokenReturns404WhenAuthorizedClientMissing() {
        OAuth2AuthorizedClientService clientService = mock(OAuth2AuthorizedClientService.class);
        DebugTokenController controller = new DebugTokenController(clientService);

        OidcIdToken idToken = new OidcIdToken(
                "id-token-value", Instant.now(), Instant.now().plusSeconds(3600), Map.of("sub", "kc-1"));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var auth =
                new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, idToken), authorities, "keycloak-web");

        when(clientService.loadAuthorizedClient("keycloak-web", auth.getName())).thenReturn(null);

        ResponseEntity<List<Map<String, Object>>> response = controller.debugToken(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void debugTokenReturns401WhenAuthenticationNull() {
        DebugTokenController controller = new DebugTokenController(mock(OAuth2AuthorizedClientService.class));

        ResponseEntity<List<Map<String, Object>>> response = controller.debugToken(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
