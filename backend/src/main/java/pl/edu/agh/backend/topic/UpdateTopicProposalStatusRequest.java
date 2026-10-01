package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;

public record UpdateTopicProposalStatusRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        TopicProposalStatus status) {}
