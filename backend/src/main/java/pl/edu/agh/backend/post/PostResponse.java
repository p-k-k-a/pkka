package pl.edu.agh.backend.post;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a single post — includes content.
 *
 * TODO: enrich with authorDisplayName after the profile issue.
 */
public record PostResponse(
        UUID id,
        String slug,
        String title,
        String content,
        String authorId,
        Instant publishedAt,
        Instant updatedAt) {

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
