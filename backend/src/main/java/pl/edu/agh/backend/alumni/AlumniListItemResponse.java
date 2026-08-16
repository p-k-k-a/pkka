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
 * alumni at once. {@code email}/{@code bio}/{@code discordId} are intentionally left off the list (only
 * shown on the single-profile view) to limit what is broadcast in bulk. Names are always present: alumni
 * who hide their name are excluded from the directory entirely ({@link AlumniSpecifications#hasVisibleName()})
 * rather than listed as nameless cards.
 */
@Schema(description = "Alumni directory list item; alumni who hide their name are excluded from the directory entirely")
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

    public static AlumniListItemResponse from(User user) {
        // Defensive only: the directory query already restricts to show_name = true
        // (AlumniSpecifications.hasVisibleName()), so the null branches below are unreachable for every user
        // that reaches this method today. Kept so the DTO cannot leak a hidden name if it is ever built
        // outside that query.
        boolean showName = user.isShowName();
        return new AlumniListItemResponse(
                user.getId(),
                showName ? user.getFirstName() : null,
                showName ? user.getLastName() : null,
                user.getCurrentPosition(),
                user.getCompany(),
                user.getLinkedinUrl(),
                user.getGithubUrl(),
                user.getGraduationYear(),
                user.isWillingToMentor(),
                user.getTags().stream()
                        .map(UserTagResponse::from)
                        .sorted(Comparator.comparing(UserTagResponse::name))
                        .toList());
    }
}
