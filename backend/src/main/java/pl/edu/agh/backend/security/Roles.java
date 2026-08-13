package pl.edu.agh.backend.security;

import lombok.experimental.UtilityClass;

/** Realm role names as Keycloak issues them, without the {@code ROLE_} authority prefix. */
@UtilityClass
public class Roles {

    public static final String USER = "USER";

    public static final String VERIFIED_ALUMN = "VERIFIED_ALUMN";

    public static final String ADMIN = "ADMIN";

    public static final String ROLE_PREFIX = "ROLE_";
}
