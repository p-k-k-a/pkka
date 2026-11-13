package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.user.User;

public record AdminTopicGroupResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,

        @Schema(
                description = "Absent once the owner's account is deleted",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Owner owner,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicGroupDiscordStatus discord,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {

    static AdminTopicGroupResponse from(TopicGroup group, boolean discordConfigured) {
        return new AdminTopicGroupResponse(
                group.getId(),
                group.getSlug(),
                group.getName(),
                group.getOwner() == null ? null : Owner.from(group.getOwner()),
                discordStatus(group, discordConfigured),
                group.getCreatedAt());
    }

    private static TopicGroupDiscordStatus discordStatus(TopicGroup group, boolean discordConfigured) {
        if (group.getDiscordRoleId() != null && group.getDiscordChannelId() != null) {
            return TopicGroupDiscordStatus.LINKED;
        }
        return discordConfigured ? TopicGroupDiscordStatus.MISSING : TopicGroupDiscordStatus.NOT_CONFIGURED;
    }

    public record Owner(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            UUID id,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String firstName,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String lastName) {

        private static Owner from(User user) {
            return new Owner(user.getId(), user.getFirstName(), user.getLastName());
        }
    }
}
