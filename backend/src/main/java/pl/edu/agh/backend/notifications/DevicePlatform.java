package pl.edu.agh.backend.notifications;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum DevicePlatform {
    ANDROID,
    IOS
}
