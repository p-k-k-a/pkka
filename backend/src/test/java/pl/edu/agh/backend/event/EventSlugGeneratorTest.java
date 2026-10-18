package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class EventSlugGeneratorTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final EventSlugGenerator generator = new EventSlugGenerator(eventRepository);

    @Test
    void slugifiesTitleWithPolishDiacritics() {
        when(eventRepository.existsBySlug(anyString())).thenReturn(false);

        assertThat(generator.generateUniqueSlug("Wieczór networkingowy alumnów"))
                .isEqualTo("wieczor-networkingowy-alumnow");
        assertThat(generator.generateUniqueSlug("Sesja Q&A z zarządem klubu")).isEqualTo("sesja-qa-z-zarzadem-klubu");
    }

    @Test
    void appendsNumericSuffixUntilSlugIsFree() {
        when(eventRepository.existsBySlug("hackathon")).thenReturn(true);
        when(eventRepository.existsBySlug("hackathon-2")).thenReturn(true);
        when(eventRepository.existsBySlug("hackathon-3")).thenReturn(false);

        assertThat(generator.generateUniqueSlug("Hackathon")).isEqualTo("hackathon-3");
    }
}
