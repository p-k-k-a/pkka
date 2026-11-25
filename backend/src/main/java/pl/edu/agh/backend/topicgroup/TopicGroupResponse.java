package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

public record TopicGroupResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) long memberCount,

        @Schema(description = "Whether the caller is a member", requiredMode = RequiredMode.REQUIRED)
        boolean member,

        @Schema(description = "Whether the caller coordinates the group", requiredMode = RequiredMode.REQUIRED)
        boolean coordinator,

        @Schema(
                description =
                        "Link to the group's Discord channel; null unless the caller is a member and the channel exists")
        String discordChannelUrl) {

    static TopicGroupResponse from(
            TopicGroup group, long memberCount, boolean member, boolean coordinator, String discordChannelUrl) {
        return new TopicGroupResponse(
                group.getId(),
                group.getSlug(),
                group.getName(),
                group.getDescription(),
                memberCount,
                member,
                coordinator,
                discordChannelUrl);
    }
}
