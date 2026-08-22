package pl.edu.agh.backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class PostSlugGeneratorTest {

    private final PostRepository postRepository = mock(PostRepository.class);
    private final PostSlugGenerator generator = new PostSlugGenerator(postRepository);

    @Test
    void slugifiesTitleWithPolishDiacritics() {
        when(postRepository.existsBySlug(anyString())).thenReturn(false);

        assertEquals("zazolc-gesla-jazn", generator.generateUniqueSlug("Zażółć gęślą jaźń!"));
        assertEquals("kolo-naukowe-ml-inzynierow", generator.generateUniqueSlug("Koło naukowe (mł. inżynierów)"));
    }

    @Test
    void appendsNumericSuffixUntilSlugIsFree() {
        when(postRepository.existsBySlug("nowy-wpis")).thenReturn(true);
        when(postRepository.existsBySlug("nowy-wpis-2")).thenReturn(true);
        when(postRepository.existsBySlug("nowy-wpis-3")).thenReturn(false);

        assertEquals("nowy-wpis-3", generator.generateUniqueSlug("Nowy wpis"));
    }

    @Test
    void truncatesOverlongTitlesToFitTheColumnWithSuffixRoom() {
        when(postRepository.existsBySlug(anyString())).thenReturn(false);

        String slug = generator.generateUniqueSlug("a".repeat(400));
        assertTrue(slug.length() <= 290);
    }
}
