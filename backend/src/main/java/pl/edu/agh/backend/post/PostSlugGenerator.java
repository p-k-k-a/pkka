package pl.edu.agh.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostSlugGenerator {

    private static final int MAX_BASE_LENGTH = 290;

    private final PostRepository postRepository;

    String generateUniqueSlug(String title) {
        String base = SlugUtils.toSlug(title);
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
