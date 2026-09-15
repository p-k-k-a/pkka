package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicProposalRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String rationale) {}
