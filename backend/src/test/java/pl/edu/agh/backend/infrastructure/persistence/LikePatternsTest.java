package pl.edu.agh.backend.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LikePatternsTest {

    @Test
    void containsEscapesSqlWildcards() {
        assertThat(LikePatterns.contains("100%_done")).isEqualTo("%100\\%\\_done%");
    }

    @Test
    void containsLowerCasesAndEscapesTheEscapeCharacter() {
        assertThat(LikePatterns.contains("A\\B")).isEqualTo("%a\\\\b%");
    }
}
