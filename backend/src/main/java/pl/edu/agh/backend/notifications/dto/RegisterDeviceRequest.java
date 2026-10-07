package pl.edu.agh.backend.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import pl.edu.agh.backend.notifications.DevicePlatform;

public record RegisterDeviceRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        DevicePlatform platform) {}
