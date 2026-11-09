package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminSurveyResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) SurveyStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<SurveyQuestionResponse> questions,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminSurveyResponse from(Survey survey) {
        return new AdminSurveyResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getDescription(),
                survey.getEndsAt(),
                survey.getStatus(),
                survey.getQuestions().stream().map(SurveyQuestionResponse::from).toList(),
                survey.getCreatedAt(),
                survey.getUpdatedAt());
    }
}
