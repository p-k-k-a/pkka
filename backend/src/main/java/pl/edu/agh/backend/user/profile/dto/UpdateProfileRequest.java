package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Fields to update; omitted or null fields clear the stored value")
public record UpdateProfileRequest(
        @Size(max = 255) String currentPosition,
        @Size(max = 255) String company,
        @Size(max = 500) String linkedinUrl,
        @Size(max = 500) String githubUrl) {}
