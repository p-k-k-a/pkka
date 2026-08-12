package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The slug is always generated from the title server-side; status defaults to DRAFT
 * so the editor can save work-in-progress without publishing.
 */
public record CreatePostRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        @Size(max = 500) String excerpt,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Markdown") @NotBlank
        String content,

        PostStatus status) {}
