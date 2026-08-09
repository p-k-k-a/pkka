package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    void returnsEmptyForUnsupportedAuthentication() {
        var auth = new UsernamePasswordAuthenticationToken("user", "pass");

        var info = extractor.extract(auth);

        assertThat(info).isEmpty();
    }
}
