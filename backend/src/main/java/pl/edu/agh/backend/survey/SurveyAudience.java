package pl.edu.agh.backend.survey;

import io.swagger.v3.oas.annotations.media.Schema;

/** Who may see and fill in a survey. */
@Schema(enumAsRef = true)
public enum SurveyAudience {
    /** Every signed-in user, verified or not. */
    ALL_USERS,
    VERIFIED_ALUMNI,
}
