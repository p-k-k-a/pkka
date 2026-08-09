package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserTagResponse;

public record ProfileResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,

        @Schema(description = "Synced from Keycloak; not editable here")
        String firstName,

        @Schema(description = "Synced from Keycloak; not editable here")
        String lastName,

        @Schema(description = "Synced from Keycloak; not editable here")
        String email,

        String currentPosition,
        String company,
        String bio,

        @Schema(description = "Discord snowflake synced from Keycloak federated identity; not editable here")
        String discordId,

        String linkedinUrl,
        String githubUrl,

        @Schema(description = "From the approved application; not editable here")
        Integer graduationYear,

        @Schema(description = "From the approved application; not editable here")
        String fieldOfStudy,

        @Schema(description = "Date the application was approved; not editable here")
        LocalDate alumnSince,

        @Schema(requiredMode = RequiredMode.REQUIRED) boolean willingToMentor,
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags,
        @Schema(requiredMode = RequiredMode.REQUIRED) ProfileVisibility visibility) {

    public static ProfileResponse from(User user, AlumnEducation education) {
        return new ProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCurrentPosition(),
                user.getCompany(),
                user.getBio(),
                user.getDiscordId(),
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
                new ProfileVisibility(user.isShowName(), user.isShowEmail(), user.isShowDiscord()));
    }
}
