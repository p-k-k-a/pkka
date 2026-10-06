package pl.edu.agh.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class CallerTest {

    private static final String KEYCLOAK_ID = "0f3a1c8e-1111-4000-8000-000000000001";

    @Test
    void readsSubjectAndRolesFromABearerToken() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(KEYCLOAK_ID)
                .build();

        Caller caller = Caller.from(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"))));

        assertThat(caller.keycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(caller.roles()).containsExactlyInAnyOrder(Roles.USER, Roles.VERIFIED_ALUMN);
        assertThat(caller.hasRole(Roles.VERIFIED_ALUMN)).isTrue();
        assertThat(caller.hasRole(Roles.ADMIN)).isFalse();
    }

    @Test
    void readsSubjectAndRolesFromAWebSession() {
        OidcIdToken idToken =
                new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", KEYCLOAK_ID));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Caller caller = Caller.from(
                new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, idToken), authorities, "keycloak"));

        assertThat(caller.keycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(caller.roles()).containsExactly(Roles.USER);
    }

    @Test
    void ignoresAuthoritiesThatAreNotRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(KEYCLOAK_ID)
                .build();

        Caller caller = Caller.from(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("SCOPE_openid"), new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(caller.roles()).containsExactly(Roles.USER);
    }

    @Test
    void treatsMissingAndAnonymousAuthenticationAsAnonymous() {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(Caller.from(null).isAnonymous()).isTrue();
        assertThat(Caller.from(anonymous).isAnonymous()).isTrue();
        assertThat(Caller.from(anonymous).roles()).isEmpty();
    }

    @Test
    void requiringAnIdentityFromAnAnonymousCallerIsUnauthorized() {
        assertThatThrownBy(() -> Caller.anonymous().requireKeycloakId())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
