package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ApplicationResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Faculty faculty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fieldOfStudy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudyType studyType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer graduationYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<MeetingPreference> meetingPreferences,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean coCreationInterest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean newsletterSubscription,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phoneNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<String> interests,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Set<ConsentType> consents,

        @Schema(description = "Set once the application is reviewed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant reviewedAt,

        @Schema(
                description = "Reason supplied when the application was rejected",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String rejectionReason,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {

    static ApplicationResponseDto from(Application a) {
        Set<String> interests = a.getInterests() == null ? new HashSet<>() : new HashSet<>(a.getInterests());
        Set<MeetingPreference> meetingPreferences =
                a.getMeetingPreferences() == null || a.getMeetingPreferences().isEmpty()
                        ? EnumSet.noneOf(MeetingPreference.class)
                        : EnumSet.copyOf(a.getMeetingPreferences());
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
                meetingPreferences,
                a.isCoCreationInterest(),
                a.isNewsletterSubscription(),
                a.getPhoneNumber(),
                interests,
                consents,
                a.getReviewedAt(),
                a.getRejectionReason(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
