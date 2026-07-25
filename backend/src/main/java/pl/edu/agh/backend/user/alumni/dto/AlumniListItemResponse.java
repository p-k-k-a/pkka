package pl.edu.agh.backend.user.alumni.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.controller.dto.TagResponse;
import pl.edu.agh.backend.user.User;

/**
 * Alumni directory list item. Exposes the same profile fields as {@code ProfileResponse} (the alumnus chose to fill
 * them in specifically to be discoverable by other alumni) plus {@code keycloakId}, which is already treated as
 * non-sensitive elsewhere in the API (e.g. {@code PostSummaryResponse#authorId} on a public endpoint). There is no
 * per-field visibility control yet (see the "hide profile fields" story) — once it exists, it plugs in here without
 * changing the field set, since fields would simply come back {@code null} when hidden.
 */
public record AlumniListItemResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Keycloak subject UUID (claim sub)")
        String keycloakId,

        String currentPosition,
        String company,
        String linkedinUrl,
        String githubUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<TagResponse> tags) {

    public static AlumniListItemResponse from(User user) {
        return new AlumniListItemResponse(
                user.getId(),
                user.getKeycloakId(),
                user.getCurrentPosition(),
                user.getCompany(),
                user.getLinkedinUrl(),
                user.getGithubUrl(),
                user.getTags().stream()
                        .map(TagResponse::from)
                        .sorted(Comparator.comparing(TagResponse::name))
                        .toList());
    }
}
