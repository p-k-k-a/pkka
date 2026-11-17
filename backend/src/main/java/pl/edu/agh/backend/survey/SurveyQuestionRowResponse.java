package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

public record SurveyQuestionRowResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String label,
        @Schema(requiredMode = RequiredMode.REQUIRED) int displayOrder) {

    static SurveyQuestionRowResponse from(SurveyQuestionRow row) {
        return new SurveyQuestionRowResponse(row.getId(), row.getLabel(), row.getDisplayOrder());
    }
}
