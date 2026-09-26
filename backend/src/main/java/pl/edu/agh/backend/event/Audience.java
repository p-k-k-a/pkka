package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum Audience {
    PUBLIC,
    ALL_ALUMNI,
    SPECIFIC_GROUP
}
