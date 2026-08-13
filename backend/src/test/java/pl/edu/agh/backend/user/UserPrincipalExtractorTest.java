package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserPrincipalExtractorTest {

    private final UserPrincipalExtractor extractor = new UserPrincipalExtractor();

    @Test
    void extractsKeycloakIdFromJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-subject-123")
                .build();
        var auth = new JwtAuthenticationToken(jwt, List.of());

        var info = extractor.extract(auth);

        assertThat(info).isPresent();
        assertThat(info.get().keycloakId()).isEqualTo("kc-subject-123");
    }

    @Test
    void extractsKeycloakIdFromOidcSessionAuthentication() {
        OidcIdToken idToken = new OidcIdToken(
                "id-token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("sub", "kc-oidc-99"));
        var oidcUser = new DefaultOidcUser(List.of(), idToken);
        var auth = new OAuth2AuthenticationToken(oidcUser, List.of(), "keycloak-web");

        var info = extractor.extract(auth);

        assertThat(info).isPresent();
        assertThat(info.get().keycloakId()).isEqualTo("kc-oidc-99");
    }

    @Test
    void returnsEmptyForUnsupportedAuthentication() {
        var auth = new UsernamePasswordAuthenticationToken("user", "pass");

        var info = extractor.extract(auth);

        assertThat(info).isEmpty();
    }
}
