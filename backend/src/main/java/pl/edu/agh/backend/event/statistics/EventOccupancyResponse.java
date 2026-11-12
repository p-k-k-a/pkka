package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventOccupancyResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,
        Integer seatLimit,
        @Schema(requiredMode = RequiredMode.REQUIRED) int registered,
        @Schema(requiredMode = RequiredMode.REQUIRED) int waitlisted,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Seat holders who cancelled; waitlist exits are not counted")
        int cancellations,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Sign-ups per source, every source listed in a fixed order, zeros included; sign-ups"
                        + " recorded before sources were are in none of them")
        List<SignUpSourceResponse> signUpsBySource) {}
