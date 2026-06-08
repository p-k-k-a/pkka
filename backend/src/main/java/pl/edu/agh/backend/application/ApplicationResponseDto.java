package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponseDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ApplicationStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String faculty,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fieldOfStudy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StudyType studyType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer graduationYear,

        @Schema(description = "Set once the application is reviewed", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant reviewedAt,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt) {

    static ApplicationResponseDto from(Application a) {
        return new ApplicationResponseDto(
                a.getId(),
                a.getStatus(),
                a.getFaculty(),
                a.getFieldOfStudy(),
                a.getStudyType(),
                a.getGraduationYear(),
                a.getReviewedAt(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
