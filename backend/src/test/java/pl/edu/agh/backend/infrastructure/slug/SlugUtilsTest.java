package pl.edu.agh.backend.infrastructure.slug;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
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

    @Test
    void uniqueSlugAppendsTheFirstFreeSuffix() {
        Set<String> taken = Set.of("hackathon", "hackathon-2");
        assertThat(SlugUtils.uniqueSlug("Hackathon", taken::contains)).isEqualTo("hackathon-3");
        assertThat(SlugUtils.uniqueSlug("Wieczór alumnów", taken::contains)).isEqualTo("wieczor-alumnow");
    }

    @Test
    void uniqueSlugLeavesRoomForASuffix() {
        assertThat(SlugUtils.uniqueSlug("a".repeat(400), slug -> false)).hasSize(250);
    }
}
