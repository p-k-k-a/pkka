package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SurveyQuestionRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
        String content,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        QuestionType type,

        @Schema(description = "The answers to choose from; the columns of a GRID question")
        List<@NotBlank @Size(max = 500) String> options,

        @Schema(description = "The rows of a GRID question; empty for every other type")
        List<@NotBlank @Size(max = 500) String> rows) {}
