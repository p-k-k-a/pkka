package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record EventOccupancyResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant endsAt,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED) int registered,
        @Schema(requiredMode = RequiredMode.REQUIRED) int waitlisted,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Seat holders who cancelled; waitlist exits are not counted")
        int cancellations) {}
