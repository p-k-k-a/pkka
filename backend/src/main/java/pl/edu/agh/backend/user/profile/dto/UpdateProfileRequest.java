package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial update: null/omitted fields are left unchanged; a blank string clears a text value")
public record UpdateProfileRequest(
        @Size(max = 2000) String bio,
        @Size(max = 255) String currentPosition,
        @Size(max = 255) String company,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 500) String githubUrl,

        @Valid
        @Schema(
                description =
                        "Visibility toggles; null leaves all unchanged, null sub-fields leave that toggle unchanged")
        VisibilityUpdate visibility) {

    public record VisibilityUpdate(Boolean name, Boolean email, Boolean discord) {}
}
