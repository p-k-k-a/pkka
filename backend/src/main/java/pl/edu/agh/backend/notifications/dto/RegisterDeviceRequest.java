package pl.edu.agh.backend.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pl.edu.agh.backend.notifications.DevicePlatform;

public record RegisterDeviceRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = RegisterDeviceRequest.EXPO_TOKEN, message = "must be an Expo push token")
        String token,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        DevicePlatform platform) {

    /** Matches {@code ExponentPushToken[…]}, the legacy {@code ExpoPushToken[…]}, or a bare UUID. */
    static final String EXPO_TOKEN =
            "^(Expo(nent)?PushToken\\[[^\\]]+\\]|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$";
}
