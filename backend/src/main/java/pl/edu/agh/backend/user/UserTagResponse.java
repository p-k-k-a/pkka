package pl.edu.agh.backend.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

public record UserTagResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Alumni listed in the directory who have this tag; only in the tag catalog")
        Long usageCount) {

    public static UserTagResponse from(UserTag tag) {
        return new UserTagResponse(tag.getId(), tag.getName(), null);
    }
}
