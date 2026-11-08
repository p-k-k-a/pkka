package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.user.User;

public record TopicGroupRequestResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TopicGroupRequestStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Requester requester,

        @Schema(description = "Set once the request is reviewed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant reviewedAt,

        @Schema(
                description = "Reason given when the request was rejected",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String rejectionReason,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt) {

    static TopicGroupRequestResponse from(TopicGroupRequest r) {
        return new TopicGroupRequestResponse(
                r.getId(),
                r.getProposedName(),
                r.getDescription(),
                r.getStatus(),
                Requester.from(r.getRequester()),
                r.getReviewedAt(),
                r.getRejectionReason(),
                r.getCreatedAt());
    }

    public record Requester(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            UUID id,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String firstName,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String lastName) {

        private static Requester from(User user) {
            return new Requester(user.getId(), user.getFirstName(), user.getLastName());
        }
    }
}
