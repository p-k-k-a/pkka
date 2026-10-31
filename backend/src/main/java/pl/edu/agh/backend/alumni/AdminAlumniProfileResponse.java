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

/** Admin version of {@link AlumniProfileResponse}. Hidden name, email and Discord are filled in. */
@Schema(description = "Alumni profile for admins; fields the alumnus hides from other alumni are still filled in")
public record AdminAlumniProfileResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        String email,
        String currentPosition,
        String company,
        String bio,

        @Schema(description = "Discord snowflake for deep links; null when not linked")
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
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags) {

    public static AdminAlumniProfileResponse from(User user, AlumnEducation education) {
        return new AdminAlumniProfileResponse(
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
                        .toList());
    }
}
