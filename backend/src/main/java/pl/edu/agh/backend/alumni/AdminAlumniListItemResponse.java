package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserTagResponse;
import pl.edu.agh.backend.user.profile.dto.ProfileVisibility;

/**
 * The admin counterpart of {@link AlumniListItemResponse}: every approved alumnus is listed with their name, and
 * {@code visibility} carries what they hide from other alumni so the UI can mark it.
 */
@Schema(description = "Alumni directory list item for admins; includes alumni who hide their name")
public record AdminAlumniListItemResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        String currentPosition,
        String company,
        String linkedinUrl,
        String githubUrl,

        @Schema(description = "From the alumnus' approved application")
        Integer graduationYear,

        @Schema(requiredMode = RequiredMode.REQUIRED) boolean willingToMentor,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "What the alumnus shows to other alumni")
        ProfileVisibility visibility) {

    public static AdminAlumniListItemResponse from(User user) {
        return new AdminAlumniListItemResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getCurrentPosition(),
                user.getCompany(),
                user.getLinkedinUrl(),
                user.getGithubUrl(),
                user.getGraduationYear(),
                user.isWillingToMentor(),
                user.getTags().stream()
                        .map(UserTagResponse::from)
                        .sorted(Comparator.comparing(UserTagResponse::name))
                        .toList(),
                new ProfileVisibility(user.isShowName(), user.isShowEmail(), user.isShowDiscord()));
    }
}
