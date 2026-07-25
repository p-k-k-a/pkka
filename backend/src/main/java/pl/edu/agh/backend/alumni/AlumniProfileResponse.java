package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.event.TagResponse;
import pl.edu.agh.backend.user.User;

@Schema(description = "Public alumni profile visible to verified alumni")
public record AlumniProfileResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        String bio,
        String discordUsername,
        String currentPosition,
        String company,
        String linkedinUrl,
        String githubUrl,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<TagResponse> tags) {

    public static AlumniProfileResponse from(User user) {
        return new AlumniProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getBio(),
                user.getDiscordUsername(),
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
