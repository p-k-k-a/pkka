package pl.edu.agh.backend.event.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record EventRegistrationResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant registeredAt,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Seats taken, including this registration")
        int seatsTaken,

        @Schema(description = "Absent when the event has no seat limit")
        Integer seatLimit) {}
