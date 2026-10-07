package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum SurveyStatus {
    DRAFT,
    ACTIVE,
    CLOSED,
}
