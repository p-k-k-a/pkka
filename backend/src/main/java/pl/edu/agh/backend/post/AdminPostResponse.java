package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminPostResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Markdown; uploaded pictures point at short-lived signed URLs, which may be "
                        + "sent back unchanged on update")
        String content,

        @Schema(
                description = "External URL, or a short-lived signed URL of an uploaded cover; sending it "
                        + "back unchanged on update is fine. While object storage is not configured an "
                        + "uploaded cover comes back as its bare `objectKey`, which cannot be saved until it is")
        String coverImageUrl,

        @Schema(requiredMode = RequiredMode.REQUIRED) PostStatus status,
        String authorDisplayName,
        Instant publishedAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    /** {@code content} and {@code coverImageUrl} are passed in already resolved to what a client can load. */
    static AdminPostResponse from(Post post, String content, String coverImageUrl) {
        return new AdminPostResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                content,
                coverImageUrl,
                post.getStatus(),
                post.getAuthor() == null ? null : post.getAuthor().getDisplayName(),
                post.getPublishedAt(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
