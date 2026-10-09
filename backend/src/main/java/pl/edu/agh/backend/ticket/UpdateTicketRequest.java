package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        TicketStatus status,

        @Schema(description = "Reply shown to the author; omit or send null to keep the current one") @Size(max = 5000)
        String adminResponse) {}
