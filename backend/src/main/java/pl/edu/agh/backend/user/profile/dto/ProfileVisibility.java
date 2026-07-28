package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record ProfileVisibility(
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean name,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean email,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean discord) {}
