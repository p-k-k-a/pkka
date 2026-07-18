package pl.edu.agh.backend.security.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record MeResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Keycloak subject UUID (claim sub)")
        String sub,

        @Schema(requiredMode = RequiredMode.REQUIRED) String email,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Given name (Keycloak claim: given_name).")
        String firstName,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Family name / surname (Keycloak claim: family_name).")
        String lastName,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Display-only username from preferred_username claim")
        String preferredUsername,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Realm roles without ROLE_ prefix")
        List<String> roles) {}
