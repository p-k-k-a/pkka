package pl.edu.agh.backend.application;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String faculty;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_preference", length = 20)
    private MeetingPreference meetingPreference;

    @Column(name = "co_creation_interest", nullable = false)
    @Builder.Default
    private boolean coCreationInterest = false;

    @Column(name = "newsletter_subscription", nullable = false)
    @Builder.Default
    private boolean newsletterSubscription = false;

    @ElementCollection(targetClass = Interest.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "application_interests",
            joinColumns = @JoinColumn(name = "application_id"),
            foreignKey = @ForeignKey(name = "fk_application_interests_application"))
    @Column(name = "interest", length = 40, nullable = false)
    @Builder.Default
    private Set<Interest> interests = EnumSet.noneOf(Interest.class);

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ApplicationConsent> consents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

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

    public void reject(User reviewer) {
        ensureUnderReview();
        this.status = ApplicationStatus.REJECTED;
        markReviewed(reviewer);
    }

    public void withdraw() {
        ensureUnderReview();
        this.status = ApplicationStatus.WITHDRAWN;
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
