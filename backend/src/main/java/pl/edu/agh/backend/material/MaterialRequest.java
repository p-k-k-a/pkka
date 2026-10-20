package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MaterialRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        MaterialType type,

        @NotBlank
        @Size(max = 2000)
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "External http(s) URL, or the `objectKey` of a file uploaded through "
                        + "`POST /api/admin/files/presign`; an uploaded file's signed URL is accepted too")
        @Pattern(
                regexp = "(?i)^(https?://|materials/).+$",
                message = "url must be an http(s) URL or the objectKey of an uploaded material")
        String url,

        UUID eventId) {}
