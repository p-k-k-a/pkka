package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserTagResponse;

/**
 * Alumni directory list item — a lighter view than {@link AlumniProfileResponse}, meant for browsing many
 * alumni at once. Fields hidden by the owner's visibility settings (currently just the name) are null,
 * same as on the single-profile view; {@code email}/{@code bio}/{@code discordId} are intentionally left
 * off the list (only shown on the single-profile view) to limit what is broadcast in bulk.
 */
@Schema(description = "Alumni directory list item; fields hidden by the owner's visibility settings are null")
public record AlumniListItemResponse(
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
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags) {

    public static AlumniListItemResponse from(User user, Integer graduationYear) {
        boolean showName = user.isShowName();
        return new AlumniListItemResponse(
                user.getId(),
                showName ? user.getFirstName() : null,
                showName ? user.getLastName() : null,
                user.getCurrentPosition(),
                user.getCompany(),
                user.getLinkedinUrl(),
                user.getGithubUrl(),
                graduationYear,
                user.isWillingToMentor(),
                user.getTags().stream()
                        .map(UserTagResponse::from)
                        .sorted(Comparator.comparing(UserTagResponse::name))
                        .toList());
    }
}
