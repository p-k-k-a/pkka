package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record EventStatisticsResponse(
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
                description = "Seat holders who cancelled; people leaving the waitlist are not counted")
        int cancellations,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "One entry per Europe/Warsaw calendar day, from publication until today or the start"
                        + " of the event, whichever is earlier; days without activity are included as zeros")
        List<DailyRegistrationsResponse> daily) {}
