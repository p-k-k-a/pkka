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
        Integer graduationYear,
        String fieldOfStudy,
        @Schema(description = "Date the alumn was approved") LocalDate alumnSince,
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
