package pl.edu.agh.backend.event.registration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum EventRegistrationStatus {
    REGISTERED,
    WAITLISTED
}
