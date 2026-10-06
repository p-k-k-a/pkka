package pl.edu.agh.backend.security.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.security.controller.dto.MeResponse;

/**
 * Unit tests for {@link MeController#me(Authentication)} covering both authentication shapes used in
 * production: web OIDC session ({@link OAuth2AuthenticationToken}) and mobile Bearer JWT
 * ({@link JwtAuthenticationToken}). {@link MeControllerIntegrationTest} exercises only the JWT path
 * via MockMvc.
 */
class MeControllerTest {

    private final MeController controller = new MeController();

    @Test
    void meReturnsClaimsFromOidcSessionUser() {
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of(
                        "sub", "kc-web-1",
                        "email", "web@example.com",
                        "given_name", "Anna",
                        "family_name", "Nowak",
                        "preferred_username", "anowak"));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        var oidcUser = new DefaultOidcUser(authorities, idToken);
        var auth = new OAuth2AuthenticationToken(oidcUser, authorities, "keycloak-web");

        MeResponse response = controller.me(auth);

        assertThat(response.sub()).isEqualTo("kc-web-1");
        assertThat(response.email()).isEqualTo("web@example.com");
        assertThat(response.firstName()).isEqualTo("Anna");
        assertThat(response.lastName()).isEqualTo("Nowak");
        assertThat(response.preferredUsername()).isEqualTo("anowak");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void meReturnsClaimsFromBearerJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-mobile-1")
                .claim("email", "mobile@example.com")
                .claim("given_name", "Jan")
                .claim("family_name", "Kowalski")
                .claim("preferred_username", "jkowalski")
                .build();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
        var auth = new JwtAuthenticationToken(jwt, authorities);

        MeResponse response = controller.me(auth);

        assertThat(response.sub()).isEqualTo("kc-mobile-1");
        assertThat(response.email()).isEqualTo("mobile@example.com");
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.roles()).containsExactly("VERIFIED_ALUMN");
    }

    @Test
    void meRejectsUnsupportedAuthenticationType() {
        var auth = new UsernamePasswordAuthenticationToken("user", "pass");

        assertThatThrownBy(() -> controller.me(auth))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meRejectsNullAuthentication() {
        assertThatThrownBy(() -> controller.me(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
