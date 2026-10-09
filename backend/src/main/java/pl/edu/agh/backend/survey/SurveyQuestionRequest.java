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

        List<@NotBlank @Size(max = 500) String> options) {}
