package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTopicGroupRequest(
        @Schema(example = "Grupa AI", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Size(max = 200)
        String name,

        @Schema(example = "Wymiana doświadczeń z wdrożeń AI.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 2000)
        String description) {}
