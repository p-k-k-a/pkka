package pl.edu.agh.backend.post;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.edu.agh.backend.user.User;

@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_slug", columnList = "slug", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * URL-friendly identyfikator posta, np. "witajcie-na-agh-alumni".
     * Generowany z tytułu przy @PrePersist jeśli nie podany.
     * Nie zmieniany przy edycji tytułu — zapobiega rotacji linków.
     */
    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(nullable = false, length = 300)
    private String title;

    /** Krótki opis na listing strony. Nullable — admin wpisuje ręcznie. */
    @Column(length = 500)
    private String excerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    /** Ustawiane automatycznie przy pierwszej zmianie statusu na PUBLISHED. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(title);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Publikuje post — ustawia status i publishedAt (tylko przy pierwszej publikacji). */
    public void publish() {
        if (this.status != PostStatus.PUBLISHED) {
            this.status = PostStatus.PUBLISHED;
            if (this.publishedAt == null) {
                this.publishedAt = Instant.now();
            }
        }
    }

    /** Cofa post do draftu. publishedAt zostaje — zachowuje oryginalną datę publikacji. */
    public void unpublish() {
        this.status = PostStatus.DRAFT;
    }
}
