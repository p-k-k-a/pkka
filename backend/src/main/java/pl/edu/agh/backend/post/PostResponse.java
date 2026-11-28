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

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Markdown; uploaded pictures point at short-lived signed URLs")
        String content,

        @Schema(description = "External URL, or a short-lived signed URL of an uploaded cover; null without a cover")
        String coverImageUrl,

        String authorId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    /** {@code content} and {@code coverImageUrl} are passed in already resolved to what a client can load. */
    static PostResponse from(Post post, String content, String coverImageUrl) {
        return new PostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                content,
                coverImageUrl,
                post.getAuthor() == null ? null : post.getAuthor().getKeycloakId(),
                post.getPublishedAt(),
                post.getUpdatedAt());
    }
}
