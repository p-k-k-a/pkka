package pl.edu.agh.backend.post;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Component
@Profile("dev")
@RequiredArgsConstructor
class PostDataInitializer implements ApplicationRunner {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (postRepository.count() > 0) return;

        User author = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            var u = new User();
            u.setKeycloakId("00000000-0000-0000-0000-000000000001");
            return userRepository.save(u);
        });

        postRepository.save(buildPost(
                author,
                "Witajcie na platformie AGH Alumni!",
                "Krótkie wprowadzenie do platformy Klubu Alumna wydziału informatyki AGH.",
                """
                ## Witajcie!

                Platforma AGH Alumni to miejsce spotkań absolwentów Wydziału Informatyki.
                Znajdziecie tu nadchodzące wydarzenia, archiwalne nagrania i możliwość \
                kontaktu z innymi alumnami.

                Zachęcamy do uzupełnienia profilu i złożenia wniosku o dołączenie do klubu.
                """));

        postRepository.save(buildPost(
                author,
                "Jak działa weryfikacja statusu alumna?",
                "Krótki przewodnik po procesie weryfikacji i nadawania statusu alumna.",
                """
                ## Proces weryfikacji

                Po rejestracji konta możesz złożyć wniosek o status alumna przez zakładkę \
                *Mój profil*. Administrator wydziału weryfikuje wniosek i nadaje odpowiednią rolę.

                Status alumna daje dostęp do pełnej listy absolwentów, wydarzeń online \
                oraz materiałów archiwalnych.
                """));

        postRepository.save(buildPost(author, "Pierwsze wydarzenie Klubu Alumna — save the date", null, """
                ## Save the date

                Już wkrótce ogłosimy datę pierwszego spotkania Klubu Alumna Wydziału \
                Informatyki AGH. Śledźcie platformę i kanał Discord!
                """));
    }

    private Post buildPost(User author, String title, String excerpt, String content) {
        var post = new Post();
        post.setTitle(title);
        post.setExcerpt(excerpt);
        post.setContent(content);
        post.setAuthor(author);
        post.publish();
        return post;
    }
}
