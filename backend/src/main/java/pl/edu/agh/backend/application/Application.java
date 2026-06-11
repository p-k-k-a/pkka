package pl.edu.agh.backend.application;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.edu.agh.backend.user.User;

@Entity
@Table(name = "applications", indexes = @Index(name = "idx_applications_status", columnList = "status, created_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.UNDER_REVIEW;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Faculty faculty;

    @NotBlank
    @Size(max = 200)
    @Column(name = "field_of_study", nullable = false, length = 200)
    private String fieldOfStudy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "study_type", nullable = false, length = 20)
    private StudyType studyType;

    @NotNull
    @Min(1919)
    @Column(name = "graduation_year", nullable = false)
    private Integer graduationYear;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_preferences", nullable = false, columnDefinition = "varchar(20)[]")
    @Builder.Default
    private List<MeetingPreference> meetingPreferences = new ArrayList<>();

    @Column(name = "co_creation_interest", nullable = false)
    @Builder.Default
    private boolean coCreationInterest = false;

    @Column(name = "newsletter_subscription", nullable = false)
    @Builder.Default
    private boolean newsletterSubscription = false;

    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^\\+?[0-9 \\-]{7,32}$")
    @Column(name = "phone_number", nullable = false, length = 32)
    private String phoneNumber;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "interests", nullable = false, columnDefinition = "varchar(100)[]")
    @Builder.Default
    private List<String> interests = new ArrayList<>();

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ApplicationConsent> consents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Size(max = 1000)
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public void addConsent(ConsentType type, Instant grantedAt) {
        ApplicationConsent consent = ApplicationConsent.builder()
                .application(this)
                .type(type)
                .grantedAt(grantedAt)
                .build();
        this.consents.add(consent);
    }

    public void approve(User reviewer) {
        ensureUnderReview();
        this.status = ApplicationStatus.APPROVED;
        markReviewed(reviewer);
    }

    public void reject(User reviewer, String reason) {
        ensureUnderReview();
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        markReviewed(reviewer);
    }

    private void ensureUnderReview() {
        if (this.status != ApplicationStatus.UNDER_REVIEW) {
            throw new InvalidApplicationStateException(this.id, this.status);
        }
    }

    private void markReviewed(User reviewer) {
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
    }
}
