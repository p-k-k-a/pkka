package pl.edu.agh.backend.alumni.contact;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.profile.dto.ProfileVisibility;

@Schema(description = "An alumn on the caller's contact list; alumni who hide their name are left out of the list")
public record ContactResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        String firstName,
        String lastName,
        String currentPosition,
        String company,
        Integer graduationYear,

        @Schema(description = "Discord snowflake for deep links; null when not linked or hidden")
        String discordId,

        @Schema(requiredMode = RequiredMode.REQUIRED) ProfileVisibility visibility) {

    static ContactResponse from(User user) {
        return new ContactResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getCurrentPosition(),
                user.getCompany(),
                user.getGraduationYear(),
                user.isShowDiscord() ? user.getDiscordId() : null,
                ProfileVisibility.of(user));
    }
}
