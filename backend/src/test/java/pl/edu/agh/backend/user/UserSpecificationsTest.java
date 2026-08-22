package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserSpecificationsTest {

    @Test
    void likePatternEscapesSqlWildcards() throws Exception {
        var method = UserSpecifications.class.getDeclaredMethod("likePattern", String.class);
        method.setAccessible(true);
        String pattern = (String) method.invoke(null, "100%_done");

        assertThat(pattern).isEqualTo("%100\\%\\_done%");
    }
}
