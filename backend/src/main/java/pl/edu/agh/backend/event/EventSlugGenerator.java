package pl.edu.agh.backend.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.edu.agh.backend.infrastructure.slug.SlugUtils;

@Component
@RequiredArgsConstructor
class EventSlugGenerator {

    private static final String FALLBACK = "wydarzenie";

    private final EventRepository eventRepository;

    String generateUniqueSlug(String title) {
        String base = SlugUtils.toSlug(title);
        if (base.isEmpty()) {
            base = FALLBACK;
        }

        String candidate = base;
        int suffix = 2;
        while (eventRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
