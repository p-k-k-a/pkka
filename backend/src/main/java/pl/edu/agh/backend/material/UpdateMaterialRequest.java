package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateMaterialRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        MaterialType type,

        @Schema(requiredMode = RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2000)
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url,

        UUID eventId) {}
