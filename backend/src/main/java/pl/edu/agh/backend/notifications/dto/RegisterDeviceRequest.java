package pl.edu.agh.backend.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.edu.agh.backend.notifications.DevicePlatform;

public record RegisterDeviceRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        @NotBlank
        @Size(max = 255)
        String token,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        DevicePlatform platform) {}
