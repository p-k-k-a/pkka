package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single post — includes content.
 *
 * TODO: enrich with authorDisplayName after the profile issue.
 */
public record PostResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getKeycloakId(),
                post.getPublishedAt(),
                post.getUpdatedAt());
    }
}
