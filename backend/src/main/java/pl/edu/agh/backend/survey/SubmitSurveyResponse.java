package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record SubmitSurveyResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID submissionId,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID surveyId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant submittedAt) {}
