package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import pl.edu.agh.backend.event.registration.RegistrationSource;

public record SignUpSourceResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) RegistrationSource source,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Sign-ups that came from this source, seat or waitlist")
        int signUps) {}
