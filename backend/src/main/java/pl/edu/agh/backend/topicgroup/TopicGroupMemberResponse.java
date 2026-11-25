package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.user.User;

@Schema(description = "A group member; the name is null when the member hides it from other alumni")
public record TopicGroupMemberResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant joinedAt,

        @Schema(description = "Whether this member coordinates the group", requiredMode = RequiredMode.REQUIRED)
        boolean coordinator) {

    static TopicGroupMemberResponse from(TopicGroupMembership membership, boolean coordinator) {
        User user = membership.getUser();
        boolean showName = user.isShowName();
        return new TopicGroupMemberResponse(
                user.getId(),
                showName ? user.getFirstName() : null,
                showName ? user.getLastName() : null,
                membership.getJoinedAt(),
                coordinator);
    }
}
