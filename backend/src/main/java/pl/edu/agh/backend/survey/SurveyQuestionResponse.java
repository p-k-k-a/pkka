package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;
import java.util.UUID;

public record SurveyQuestionResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = RequiredMode.REQUIRED) QuestionType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) int displayOrder,
        List<SurveyQuestionOptionResponse> options,
        List<SurveyQuestionRowResponse> rows) {

    static SurveyQuestionResponse from(SurveyQuestion question) {
        List<SurveyQuestionOptionResponse> options = question.getType() == QuestionType.TEXT
                ? List.of()
                : question.getOptions().stream()
                        .map(SurveyQuestionOptionResponse::from)
                        .toList();
        List<SurveyQuestionRowResponse> rows = question.getType() == QuestionType.GRID
                ? question.getRows().stream()
                        .map(SurveyQuestionRowResponse::from)
                        .toList()
                : List.of();
        return new SurveyQuestionResponse(
                question.getId(), question.getContent(), question.getType(), question.getDisplayOrder(), options, rows);
    }
}
