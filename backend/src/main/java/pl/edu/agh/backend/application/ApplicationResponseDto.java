package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ApplicationResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String faculty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fieldOfStudy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudyType studyType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer graduationYear,

        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        MeetingPreference meetingPreference,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean coCreationInterest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean newsletterSubscription,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<Interest> interests,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<ConsentType> consents,

        @Schema(description = "Set once the application is reviewed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant reviewedAt,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {

    static ApplicationResponseDto from(Application a) {
        Set<Interest> interests = a.getInterests() == null || a.getInterests().isEmpty()
                ? EnumSet.noneOf(Interest.class)
                : EnumSet.copyOf(a.getInterests());
        Set<ConsentType> consents = a.getConsents() == null
                ? EnumSet.noneOf(ConsentType.class)
                : a.getConsents().stream()
                        .map(ApplicationConsent::getType)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(ConsentType.class)));

        return new ApplicationResponseDto(
                a.getId(),
                a.getStatus(),
                a.getFaculty(),
                a.getFieldOfStudy(),
                a.getStudyType(),
                a.getGraduationYear(),
                a.getMeetingPreference(),
                a.isCoCreationInterest(),
                a.isNewsletterSubscription(),
                interests,
                consents,
                a.getReviewedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
