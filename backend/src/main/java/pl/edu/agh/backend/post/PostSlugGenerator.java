package pl.edu.agh.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostSlugGenerator {

    /** Room left for a "-<n>" uniqueness suffix within the 300-char column. */
    private static final int MAX_BASE_LENGTH = 290;

    private static final String FALLBACK_SLUG = "wpis";

    private final PostRepository postRepository;

    /**
     * Slugifies the title and de-duplicates against existing posts by appending
     * "-2", "-3", ... so two posts titled the same never collide on the unique slug column.
     */
    String generateUniqueSlug(String title) {
        String base = SlugUtils.toSlug(title);
        if (base.isBlank()) {
            base = FALLBACK_SLUG;
        }
        if (base.length() > MAX_BASE_LENGTH) {
            base = base.substring(0, MAX_BASE_LENGTH);
        }

        String candidate = base;
        int suffix = 2;
        while (postRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
