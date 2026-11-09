package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import pl.edu.agh.backend.user.User;

public record ProfileVisibility(
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean name,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean email,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean discord,
        @Schema(requiredMode = RequiredMode.REQUIRED) boolean phone) {

    public static ProfileVisibility of(User user) {
        return new ProfileVisibility(user.isShowName(), user.isShowEmail(), user.isShowDiscord(), user.isShowPhone());
    }
}
