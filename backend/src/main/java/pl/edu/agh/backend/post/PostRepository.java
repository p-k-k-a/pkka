package pl.edu.agh.backend.post;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /** Public view - only published posts, paged. */
    Page<Post> findAllByStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    /** Get post by slug (published only - for the public API). */
    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    /** Get post by slug regardless of status (admin panel). */
    Optional<Post> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
