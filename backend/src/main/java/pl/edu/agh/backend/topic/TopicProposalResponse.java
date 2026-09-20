package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record TopicProposalResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) String rationale,
        @Schema(requiredMode = RequiredMode.REQUIRED) TopicProposalStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorDisplayName,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt) {

    static TopicProposalResponse from(TopicProposal proposal) {
        return new TopicProposalResponse(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getDescription(),
                proposal.getRationale(),
                proposal.getStatus(),
                proposal.getAuthor().getDisplayName(),
                proposal.getCreatedAt());
    }
}
