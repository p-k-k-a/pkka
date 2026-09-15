package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminSurveyListItemResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) SurveyStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) int questionCount,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt) {

    static AdminSurveyListItemResponse from(Survey survey) {
        return new AdminSurveyListItemResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getEndsAt(),
                survey.getStatus(),
                survey.getQuestions().size(),
                survey.getCreatedAt());
    }
}
