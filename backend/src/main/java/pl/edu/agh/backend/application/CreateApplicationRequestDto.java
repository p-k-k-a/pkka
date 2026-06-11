package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public record CreateApplicationRequestDto(
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

        @Schema(description = "Newsletter subscription", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        boolean newsletterSubscription,

        @Schema(description = "Phone number", example = "+48 123 456 789", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^\\+?[0-9 \\-]{7,32}$")
        String phoneNumber,

        @Schema(
                description = "Required consents — must include REGULATIONS_PRIVACY and GDPR_DATA_PROCESSING",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        Set<ConsentType> consents) {

    public CreateApplicationRequestDto {
        interests = interests == null || interests.isEmpty() ? new HashSet<>() : new HashSet<>(interests);
        meetingPreferences = meetingPreferences == null || meetingPreferences.isEmpty()
                ? EnumSet.noneOf(MeetingPreference.class)
                : EnumSet.copyOf(meetingPreferences);
        consents =
                consents == null || consents.isEmpty() ? EnumSet.noneOf(ConsentType.class) : EnumSet.copyOf(consents);
    }

    @AssertTrue(message = "Both required consents must be granted: REGULATIONS_PRIVACY and GDPR_DATA_PROCESSING")
    @Schema(hidden = true)
    public boolean isAllRequiredConsentsGranted() {
        return consents.containsAll(ConsentType.REQUIRED);
    }
}
