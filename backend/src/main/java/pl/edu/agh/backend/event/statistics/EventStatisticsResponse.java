package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventStatisticsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID eventId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant startsAt,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "When the event was created, i.e. made visible")
        Instant publishedAt,

        Integer seatLimit,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "People holding a seat right now")
        int registered,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "People queuing for a seat right now")
        int waitlisted,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Every sign-up ever made, including those later cancelled and those that joined the"
                        + " waitlist")
        int signUps,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Every withdrawal: seat holders who cancelled and people who left the waitlist")
        int cancellations,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "The part of `cancellations` that left the waitlist")
        int cancellationsFromWaitlist,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Waitlisted people who were moved onto a seat")
        int promotions,

        @Schema(
                description = "Held seats as a percentage of the seat limit, rounded; absent when the event has no"
                        + " limit. Can exceed 100 when the limit was lowered below the seats already taken.")
        Integer occupancyPercent,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "One entry per Europe/Warsaw calendar day, from publication until today or the start"
                        + " of the event, whichever is earlier; days without activity are included as zeros")
        List<DailyRegistrationsResponse> daily) {}
