package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record RejectApplicationRequestDto(
        @Schema(
                description = "Optional reason shown to the applicant",
                example = "Brak udokumentowanego ukończenia studiów",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 1000)
        String reason) {}
