package pl.edu.agh.backend.event.registration;

import io.swagger.v3.oas.annotations.media.Schema;

/** Where a sign-up came from: the client it was made in, or the link that led there. */
@Schema(enumAsRef = true)
public enum RegistrationSource {
    WEB,
    MOBILE,
    DISCORD,
    EMAIL
}
