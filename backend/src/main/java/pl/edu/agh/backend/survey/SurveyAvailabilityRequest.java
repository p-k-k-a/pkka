package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** The part of a survey that stays editable once people have answered it: when it is open. */
public record SurveyAvailabilityRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        SurveyStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant endsAt) {}
