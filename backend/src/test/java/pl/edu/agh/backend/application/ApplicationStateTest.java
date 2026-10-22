package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.edu.agh.backend.user.User;

class ApplicationStateTest {

    private User applicant;
    private User reviewer;
    private Application application;

    @BeforeEach
    void setUp() {
        applicant = new User();
        applicant.setKeycloakId(UUID.randomUUID().toString());

        reviewer = new User();
        reviewer.setKeycloakId(UUID.randomUUID().toString());

        application = Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .build();
    }

    @Test
    void approveTransitionsToApprovedAndDenormalizesGraduationYear() {
        application.approve(reviewer);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getReviewedBy()).isEqualTo(reviewer);
        assertThat(application.getReviewedAt()).isNotNull();
        assertThat(application.getRejectionReason()).isNull();
        assertThat(applicant.getGraduationYear()).isEqualTo(2020);
    }

    @Test
    void rejectTransitionsToRejectedWithReason() {
        application.reject(reviewer, "Incomplete documentation");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getRejectionReason()).isEqualTo("Incomplete documentation");
        assertThat(application.getReviewedBy()).isEqualTo(reviewer);
        assertThat(application.getReviewedAt()).isNotNull();
        assertThat(applicant.getGraduationYear()).isNull();
    }

    @Test
    void approveOnAlreadyApprovedThrows() {
        application.approve(reviewer);

        assertThatThrownBy(() -> application.approve(reviewer))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("already APPROVED");
    }

    @Test
    void rejectOnAlreadyRejectedThrows() {
        application.reject(reviewer, "reason");

        assertThatThrownBy(() -> application.reject(reviewer, "again"))
                .isInstanceOf(InvalidApplicationStateException.class)
                .hasMessageContaining("already REJECTED");
    }
}
