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

        @Size(max = 32)
        @Pattern(regexp = "^$|^\\+?[0-9 \\-]{7,32}$", message = "must be a phone number, e.g. +48 600 700 800")
        @Schema(example = "+48 600 700 800")
        String phoneNumber,

        @Schema(description = "Willingness to mentor other alumni; null leaves it unchanged")
        Boolean willingToMentor,

        @Valid
        @Schema(
                description =
                        "Visibility toggles; null leaves all unchanged, null sub-fields leave that toggle unchanged")
        VisibilityUpdate visibility,

        @Size(max = 255)
        @Schema(
                description = "`objectKey` returned by the avatar presign endpoint, once the file is uploaded; "
                        + "a blank string removes the avatar. Any other value is rejected")
        String avatarKey) {

    public record VisibilityUpdate(Boolean name, Boolean email, Boolean discord, Boolean phone) {}
}
