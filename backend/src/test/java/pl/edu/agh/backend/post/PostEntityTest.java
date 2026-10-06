package pl.edu.agh.backend.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.edu.agh.backend.user.User;

class PostEntityTest {

    @Test
    void prePersistGeneratesSlugFromTitle() {
        Post post = new Post();
        post.setTitle("Welcome to AGH Alumni!");
        post.setContent("body");
        post.setAuthor(new User());

        post.onCreate();

        assertThat(post.getSlug()).isEqualTo("welcome-to-agh-alumni");
    }

    @Test
    void publishSetsStatusAndPublishedAtOnce() {
        Post post = new Post();
        post.setTitle("Draft");
        post.setContent("body");
        post.setAuthor(new User());

        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isNotNull();
    }

    @Test
    void publishIsIdempotentAndKeepsTheFirstPublishedAt() {
        Post post = new Post();
        post.setTitle("Draft");
        post.setContent("body");
        post.setAuthor(new User());
        post.publish();
        Instant firstPublishedAt = post.getPublishedAt();

        post.publish();

        assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(post.getPublishedAt()).isEqualTo(firstPublishedAt);
    }
}
