package pl.edu.agh.backend.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SlugUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "'Welcome to AGH Alumni!', welcome-to-agh-alumni",
        "'  Multiple   Spaces  ', multiple-spaces",
        "'Special @#$ Characters!', special-characters",
        "'Already-a-slug', already-a-slug",
        "'Double--Dash', double-dash"
    })
    void toSlugNormalizesInput(String input, String expected) {
        assertThat(SlugUtils.toSlug(input)).isEqualTo(expected);
    }

    @Test
    void toSlugStripsPolishDiacritics() {
        assertThat(SlugUtils.toSlug("Kraków")).isEqualTo("krakow");
    }

    @Test
    void toSlugHandlesEmptyAfterStripping() {
        assertThat(SlugUtils.toSlug("!!!")).isEmpty();
    }
}
