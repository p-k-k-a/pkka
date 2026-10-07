package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum MeetingPreference {
    ONLINE,
    IN_PERSON_KRAKOW,
    HYBRID
}
