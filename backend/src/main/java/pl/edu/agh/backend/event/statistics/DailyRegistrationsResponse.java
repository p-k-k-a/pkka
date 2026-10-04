package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;

public record DailyRegistrationsResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate date,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Sign-ups made that day, seat or waitlist")
        int signUps,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Cancellations and waitlist exits that day")
        int cancellations) {}
