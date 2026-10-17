package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateSurveyRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        String description,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant endsAt,

        SurveyStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty @Valid
        List<SurveyQuestionRequest> questions) {}
