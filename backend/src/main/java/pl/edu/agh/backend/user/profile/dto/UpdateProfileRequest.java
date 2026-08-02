package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Partial update: null/omitted fields are left unchanged; a blank string clears a text value")
public record UpdateProfileRequest(
        @Size(max = 2000) String bio,
        @Size(max = 255) String currentPosition,
        @Size(max = 255) String company,

        @Size(max = 500)
        @Pattern(
                regexp = "(?i)^$|^https://([a-z]{2,3}\\.)?linkedin\\.com/in/[^/?#]+/?$",
                message = "must be a LinkedIn profile URL, e.g. https://www.linkedin.com/in/jan-kowalski")
        @Schema(example = "https://www.linkedin.com/in/jan-kowalski")
        String linkedinUrl,

        @Size(max = 500)
        @Pattern(
                regexp = "(?i)^$|^https://(www\\.)?github\\.com/[A-Za-z0-9](([A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38})/?$",
                message = "must be a GitHub profile URL, e.g. https://github.com/jankowalski")
        @Schema(example = "https://github.com/jankowalski")
        String githubUrl,

        @Valid
        @Schema(
                description =
                        "Visibility toggles; null leaves all unchanged, null sub-fields leave that toggle unchanged")
        VisibilityUpdate visibility) {

    public record VisibilityUpdate(Boolean name, Boolean email, Boolean discord) {}
}
