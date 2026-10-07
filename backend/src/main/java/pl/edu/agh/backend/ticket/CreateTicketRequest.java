package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        TicketCategory category,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 300)
        String title,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 5000)
        String description) {}
