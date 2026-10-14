package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

/** A survey as shown in the alumni list: no questions, those come with {@link SurveyResponse}. */
public record SurveyListItemResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) SurveyStatus status) {

    static SurveyListItemResponse from(Survey survey) {
        return new SurveyListItemResponse(
                survey.getId(), survey.getTitle(), survey.getDescription(), survey.getEndsAt(), survey.getStatus());
    }
}
