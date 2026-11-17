package pl.edu.agh.backend.topicgroup;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.edu.agh.backend.user.User;

/**
 * An alumn's proposal for a new {@link TopicGroup}, reviewed by an admin exactly like an
 * {@link pl.edu.agh.backend.application.Application}: approving it creates the group, rejecting it records why.
 */
@Entity
@Table(
        name = "topic_group_requests",
        indexes = @Index(name = "idx_topic_group_requests_status", columnList = "status, created_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class TopicGroupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** What the group is to be called; the name is only taken once the request is approved. */
    @NotBlank
    @Size(max = 200)
    @Column(name = "proposed_name", nullable = false, length = 200)
    @ToString.Include
    private String proposedName;

    /** Becomes the group's description, and is what the reviewing admin judges the proposal by. */
    @NotBlank
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @ToString.Include
    @Builder.Default
    private TopicGroupRequestStatus status = TopicGroupRequestStatus.UNDER_REVIEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Size(max = 1000)
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    /** The group this request produced; absent until it is approved. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_group_id")
    private TopicGroup createdGroup;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void approve(User reviewer, TopicGroup created) {
        ensureUnderReview();
        this.status = TopicGroupRequestStatus.APPROVED;
        this.createdGroup = created;
        markReviewed(reviewer);
    }

    public void reject(User reviewer, String reason) {
        ensureUnderReview();
        this.status = TopicGroupRequestStatus.REJECTED;
        this.rejectionReason = reason;
        markReviewed(reviewer);
    }

    private void ensureUnderReview() {
        if (this.status != TopicGroupRequestStatus.UNDER_REVIEW) {
            throw new InvalidTopicGroupRequestStateException(this.id, this.status);
        }
    }

    private void markReviewed(User reviewer) {
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicGroupRequest other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
