package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserTagResponse;
import pl.edu.agh.backend.user.profile.dto.ProfileVisibility;

/**
 * Public alumni profile. The education facts ({@code graduationYear}, {@code fieldOfStudy}, {@code alumnSince})
 * are required here, unlike in {@link pl.edu.agh.backend.user.profile.dto.ProfileResponse}: this endpoint 404s
 * for anyone without an approved application (see {@link AlumniService#getProfile}), and an approved application
 * always carries all three — the first two are NOT NULL columns, and {@code Application.approve()} stamps
 * {@code reviewedAt}, which {@code alumnSince} is derived from. The own-profile response serves users who may
 * have no approved application at all, so it leaves the same three optional.
 */
@Schema(
        description =
                "Public alumni profile visible to verified alumni; fields hidden by the owner's visibility settings are null")
public record AlumniProfileResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        String email,
        String currentPosition,
        String company,
        String bio,

        @Schema(description = "Discord snowflake for deep links; null when not linked or hidden")
        String discordId,

        String linkedinUrl,
        String githubUrl,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "From the alumnus' approved application")
        Integer graduationYear,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "From the alumnus' approved application")
        String fieldOfStudy,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Date the alumn was approved")
        LocalDate alumnSince,

        @Schema(requiredMode = RequiredMode.REQUIRED) boolean willingToMentor,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags,
        @Schema(requiredMode = RequiredMode.REQUIRED) ProfileVisibility visibility) {

    public static AlumniProfileResponse from(User user, AlumnEducation education) {
        boolean showName = user.isShowName();
        boolean showEmail = user.isShowEmail();
        boolean showDiscord = user.isShowDiscord();
        return new AlumniProfileResponse(
                user.getId(),
                showName ? user.getFirstName() : null,
                showName ? user.getLastName() : null,
                showEmail ? user.getEmail() : null,
                user.getCurrentPosition(),
                user.getCompany(),
                user.getBio(),
                showDiscord ? user.getDiscordId() : null,
                user.getLinkedinUrl(),
                user.getGithubUrl(),
                education.graduationYear(),
                education.fieldOfStudy(),
                education.alumnSince(),
                user.isWillingToMentor(),
                user.getTags().stream()
                        .map(UserTagResponse::from)
                        .sorted(Comparator.comparing(UserTagResponse::name))
                        .toList(),
                new ProfileVisibility(showName, showEmail, showDiscord));
    }
}
