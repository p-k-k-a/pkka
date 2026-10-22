package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record CreateApplicationRequest(
        @Schema(description = "AGH faculty code", example = "WI", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        Faculty faculty,

        @Schema(description = "Field of study", example = "Informatyka", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String fieldOfStudy,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @NotNull
        StudyType studyType,

        @Schema(description = "Year of graduation", example = "2020", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Min(1919)
        Integer graduationYear,

        @Schema(
                description = "Free-form areas of interest",
                example = "[\"AI\",\"backend\"]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Set<@NotBlank @Size(max = 100) String> interests,

        @Schema(description = "Acceptable meeting formats", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Set<MeetingPreference> meetingPreferences,

        @Schema(
                description = "Willingness to actively co-create the club",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean coCreationInterest,

        @Schema(description = "Phone number", example = "+48 123 456 789", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^\\+?[0-9 \\-]{7,32}$")
        String phoneNumber,

        @Schema(
                description = "Ids of the accepted legal document versions; must be the ones in force and cover every"
                        + " required type. The newsletter consent is optional.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        Set<@NotNull UUID> acceptedDocumentIds) {

    public CreateApplicationRequest {
        interests = interests == null || interests.isEmpty() ? new HashSet<>() : new HashSet<>(interests);
        meetingPreferences = meetingPreferences == null || meetingPreferences.isEmpty()
                ? EnumSet.noneOf(MeetingPreference.class)
                : EnumSet.copyOf(meetingPreferences);
        acceptedDocumentIds = acceptedDocumentIds == null ? new HashSet<>() : new HashSet<>(acceptedDocumentIds);
    }
}
