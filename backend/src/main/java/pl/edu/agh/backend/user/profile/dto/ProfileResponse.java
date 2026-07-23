package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.event.TagResponse;
import pl.edu.agh.backend.user.User;

public record ProfileResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String currentPosition,
        String company,
        String linkedinUrl,
        String githubUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<TagResponse> tags) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getId(),
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
