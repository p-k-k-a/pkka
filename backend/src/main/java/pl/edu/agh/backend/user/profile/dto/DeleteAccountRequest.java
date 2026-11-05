package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, description = "E-mail of the account, typed again to confirm")
        @NotBlank
        String email) {}
