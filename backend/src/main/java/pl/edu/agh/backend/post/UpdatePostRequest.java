package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Full replacement of the editable fields. The slug never changes to avoid link rotation. */
public record UpdatePostRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        @Size(max = 500) String excerpt,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Markdown") @NotBlank
        String content,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        PostStatus status) {}
