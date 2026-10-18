package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;
import java.util.UUID;

public record SurveyResultsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID surveyId,
        @Schema(requiredMode = RequiredMode.REQUIRED) int submissionCount,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<QuestionResult> questions) {

    public record QuestionResult(
            @Schema(requiredMode = RequiredMode.REQUIRED) UUID questionId,
            @Schema(requiredMode = RequiredMode.REQUIRED) String content,
            @Schema(requiredMode = RequiredMode.REQUIRED) QuestionType type,
            List<OptionCount> optionCounts,
            List<String> textAnswers) {}

    public record OptionCount(
            @Schema(requiredMode = RequiredMode.REQUIRED) UUID optionId,
            @Schema(requiredMode = RequiredMode.REQUIRED) String label,
            @Schema(requiredMode = RequiredMode.REQUIRED) long count) {}
}
