package pl.edu.agh.backend.alumni;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.user.User;

/** Another alumn's picture is shown only while they show their name. */
class AlumniAvatarTest {

    private static final String AVATAR_URL = "https://r2.example/avatars/a.png?X-Amz-Signature=x";
    private static final AlumnEducation EDUCATION = new AlumnEducation(2020, "Informatyka", LocalDate.of(2024, 1, 1));

    @Test
    void anAlumnWhoShowsTheirNameShowsTheirPicture() {
        User user = user(true);

        assertThat(AlumniProfileResponse.from(user, EDUCATION, false, AVATAR_URL)
                        .avatarUrl())
                .isEqualTo(AVATAR_URL);
        assertThat(AlumniListItemResponse.from(user, AVATAR_URL).avatarUrl()).isEqualTo(AVATAR_URL);
    }

    @Test
    void anAlumnWhoHidesTheirNameHidesTheirPictureToo() {
        User user = user(false);

        assertThat(AlumniProfileResponse.from(user, EDUCATION, false, AVATAR_URL)
                        .avatarUrl())
                .isNull();
        assertThat(AlumniListItemResponse.from(user, AVATAR_URL).avatarUrl()).isNull();
    }

    private static User user(boolean showName) {
        User user = new User();
        user.setFirstName("Jan");
        user.setLastName("Kowalski");
        user.setShowName(showName);
        return user;
    }
}
