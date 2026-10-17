package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;

public record DailyRegistrationsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate date,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Sign-ups made that day, seat or waitlist")
        int signUps,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Seat holders who cancelled that day")
        int cancellations,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "People holding a seat or waiting on the waitlist at the end of that day")
        int signedUpAtEndOfDay) {}
