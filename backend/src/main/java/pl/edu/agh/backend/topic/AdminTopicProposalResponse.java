package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminTopicProposalResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) String rationale,
        @Schema(requiredMode = RequiredMode.REQUIRED) TopicProposalStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID authorId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorDisplayName,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminTopicProposalResponse from(TopicProposal proposal) {
        return new AdminTopicProposalResponse(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getDescription(),
                proposal.getRationale(),
                proposal.getStatus(),
                proposal.getAuthor().getId(),
                proposal.getAuthor().getDisplayName(),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt());
    }
}
