package pl.edu.agh.backend.user;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

public record UserTagResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name) {

    public static UserTagResponse from(UserTag tag) {
        return new UserTagResponse(tag.getId(), tag.getName());
    }
}
