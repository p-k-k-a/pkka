package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserTagResponse;

/** Admin version of {@link AlumniListItemResponse}. It also lists alumni who hide their name. */
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
        @Schema(requiredMode = RequiredMode.REQUIRED) List<UserTagResponse> tags) {

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
                        .toList());
    }
}
