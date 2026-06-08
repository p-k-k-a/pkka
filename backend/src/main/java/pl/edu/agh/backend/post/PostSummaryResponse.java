package pl.edu.agh.backend.post;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
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
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String slug,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorId,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant publishedAt) {

    static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(), post.getSlug(), post.getTitle(), post.getAuthor().getKeycloakId(), post.getPublishedAt());
    }
}
