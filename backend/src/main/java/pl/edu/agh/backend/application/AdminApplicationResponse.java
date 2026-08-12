package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record AdminApplicationResponse(
        @Schema(description = "Local id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID applicantId,

        @Schema(description = "Keycloak id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicantKeycloakId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationResponse application) {

    static AdminApplicationResponse from(Application a) {
        return new AdminApplicationResponse(
                a.getApplicant().getId(), a.getApplicant().getKeycloakId(), ApplicationResponse.from(a));
    }
}
