package pl.edu.agh.backend.security.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

public record MeResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) String username,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = RequiredMode.REQUIRED) String email,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<String> roles) {}
