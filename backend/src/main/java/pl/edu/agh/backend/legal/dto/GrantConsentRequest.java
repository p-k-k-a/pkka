package pl.edu.agh.backend.legal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GrantConsentRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Must be the current version of its type") @NotNull
        UUID documentId) {}
