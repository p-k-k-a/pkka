package pl.edu.agh.backend.support;

import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class JwtTestSupport {

    private JwtTestSupport() {}

    public static RequestPostProcessor asUser(String keycloakId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(keycloakId))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static RequestPostProcessor asUser() {
        return asUser(UUID.randomUUID().toString());
    }

    public static RequestPostProcessor asVerifiedAlumn() {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .authorities(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
    }

    public static RequestPostProcessor asAdmin(String keycloakId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(keycloakId))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public static RequestPostProcessor asAdmin() {
        return asAdmin(UUID.randomUUID().toString());
    }
}
