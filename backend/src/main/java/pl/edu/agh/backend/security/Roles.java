package pl.edu.agh.backend.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@UtilityClass
public class Roles {

    public static final String USER = "USER";

    public static final String VERIFIED_ALUMN = "VERIFIED_ALUMN";

    public static final String ADMIN = "ADMIN";

    public static final String ROLE_PREFIX = "ROLE_";

    public boolean has(Authentication authentication, String role) {
        String authority = ROLE_PREFIX + role;
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority::equals);
    }
}
