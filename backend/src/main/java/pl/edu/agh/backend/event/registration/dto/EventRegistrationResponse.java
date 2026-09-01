package pl.edu.agh.backend.event.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;

public record EventRegistrationResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant registeredAt,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Whether this sign-up holds a seat or is queuing for one")
        EventRegistrationStatus status,

        @Schema(description = "Place in the queue, counting from 1; absent unless WAITLISTED")
        Integer waitlistPosition,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Confirmed registrations, not counting the queue")
        int seatsTaken,

        @Schema(description = "Absent when the event has no seat limit")
        Integer seatLimit) {}
