package pl.edu.agh.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SecurityConfigRolesTest {

    @Test
    void extractRealmRolesMapsDashToUnderscoreAndUppercases() {
        Map<String, Object> claims = Map.of("realm_access", Map.of("roles", List.of("verified-alumn", "admin")));

        Set<String> roles = SecurityConfig.extractRealmRoles(claims).stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        assertThat(roles).containsExactlyInAnyOrder("ROLE_VERIFIED_ALUMN", "ROLE_ADMIN");
    }

    @Test
    void extractRealmRolesReturnsEmptyWhenClaimMissing() {
        assertThat(SecurityConfig.extractRealmRoles(Map.of())).isEmpty();
    }

    @Test
    void extractRealmRolesReturnsEmptyWhenRolesNull() {
        Map<String, Object> claims = Map.of("realm_access", Map.of());
        assertThat(SecurityConfig.extractRealmRoles(claims)).isEmpty();
    }

    @Test
    void hasBearerTokenDetectsAuthorizationHeader() {
        var request = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        org.mockito.Mockito.when(request.getHeader("Authorization")).thenReturn("Bearer abc");

        assertThat(SecurityConfig.hasBearerToken(request)).isTrue();
    }

    @Test
    void hasBearerTokenReturnsFalseWithoutBearer() {
        var request = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        org.mockito.Mockito.when(request.getHeader("Authorization")).thenReturn("Basic abc");

        assertThat(SecurityConfig.hasBearerToken(request)).isFalse();
    }
}
