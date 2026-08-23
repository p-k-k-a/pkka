package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminPostResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = RequiredMode.REQUIRED) PostStatus status,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorId,
        Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminPostResponse from(Post post) {
        return new AdminPostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                post.getAuthor().getKeycloakId(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
