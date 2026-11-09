package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import pl.edu.agh.backend.user.User;

public record AdminApplicationResponse(
        @Schema(description = "Local id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID applicantId,

        @Schema(description = "Keycloak id of the applicant", requiredMode = Schema.RequiredMode.REQUIRED)
        String applicantKeycloakId,

        @Schema(
                description = "Who decided this application. Absent while it waits, and absent on applications "
                        + "approved on the strength of the applicant's Discord roles, which nobody reviewed.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Reviewer reviewedBy,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationResponse application) {

    static AdminApplicationResponse from(Application a) {
        return new AdminApplicationResponse(
                a.getApplicant().getId(),
                a.getApplicant().getKeycloakId(),
                Reviewer.from(a.getReviewedBy()),
                ApplicationResponse.from(a));
    }

    public record Reviewer(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            UUID id,

            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String firstName,

            @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String lastName) {

        private static Reviewer from(User reviewer) {
            return reviewer == null
                    ? null
                    : new Reviewer(reviewer.getId(), reviewer.getFirstName(), reviewer.getLastName());
        }
    }
}
