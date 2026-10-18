package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SubmitSurveyRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty @Valid
        List<AnswerInput> answers) {

    public record AnswerInput(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            UUID questionId,

            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank
            String value) {}
}
