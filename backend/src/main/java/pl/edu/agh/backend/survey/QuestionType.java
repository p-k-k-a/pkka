package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum QuestionType {
    SINGLE_CHOICE,
    MULTI_CHOICE,
    TEXT,
    /** Rows answered with one option each, like a rating of several things on one scale. */
    GRID,
}
