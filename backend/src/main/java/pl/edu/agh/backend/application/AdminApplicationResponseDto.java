package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record AdminApplicationResponseDto(
        @Schema(description = "Local id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID applicantId,

        @Schema(description = "Keycloak id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicantKeycloakId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationResponseDto application) {

    static AdminApplicationResponseDto from(Application a) {
        return new AdminApplicationResponseDto(
                a.getApplicant().getId(), a.getApplicant().getKeycloakId(), ApplicationResponseDto.from(a));
    }
}
