package pl.edu.agh.backend.post;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for a list of posts — without the content field.
 *
 * authorId is the author's Keycloak ID. First/last name are not available
 * until the profile issue is completed (these details live in Keycloak, not in the app DB).
 * TODO: enrich with authorDisplayName after the profile issue.
 */
public record PostSummaryResponse(
        UUID id, String slug, String title, String excerpt, String authorId, Instant publishedAt) {

    static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getAuthor().getKeycloakId(),
                post.getPublishedAt());
    }
}
