package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SubmitSurveyRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotEmpty @Valid
        List<AnswerInput> answers) {

    public record AnswerInput(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            UUID questionId,

            // Wide enough for every option id of a multiple choice; a text answer has no other bound.
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 5000)
            String value) {}
}
