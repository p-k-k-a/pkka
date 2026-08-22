package pl.edu.agh.backend.post;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.edu.agh.backend.user.User;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_slug", columnList = "slug", unique = true))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * URL-friendly post identifier, e.g. "welcome-to-agh-alumni".
     * Generated from the title on @PrePersist if not provided.
     * Not updated when the title changes to avoid link rotation.
     */
    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    /** Set automatically on the first status change to PUBLISHED. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(title);
        }
    }

    /** Publishes the post, setting status and publishedAt (only on first publication). */
    public void publish() {
        if (this.status != PostStatus.PUBLISHED) {
            this.status = PostStatus.PUBLISHED;
            if (this.publishedAt == null) {
                this.publishedAt = Instant.now();
            }
        }
    }

    /** Reverts the post to draft. publishedAt remains to preserve the original publication date. */
    public void unpublish() {
        this.status = PostStatus.DRAFT;
    }
}
