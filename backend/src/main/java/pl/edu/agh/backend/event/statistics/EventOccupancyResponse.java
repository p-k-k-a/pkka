package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record EventOccupancyResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED) int registered,
        @Schema(requiredMode = RequiredMode.REQUIRED) int waitlisted,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Seat cancellations and waitlist exits together")
        int cancellations,

        @Schema(description = "Held seats as a percentage of the seat limit, rounded; absent when there is no limit")
        Integer occupancyPercent) {}
