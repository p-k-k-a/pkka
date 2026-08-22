package pl.edu.agh.backend.post;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.user.User;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void findAllPublishedMapsOnlyPublishedPosts() {
        Post post = new Post();
        post.setTitle("Hello");
        post.setContent("Body");
        post.setAuthor(new User());
        post.publish();
        when(postRepository.findAllByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(post)));

        var page = postService.findAllPublished(PageRequest.of(0, 10));

        org.assertj.core.api.Assertions.assertThat(page.getContent()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(page.getContent().getFirst().title())
                .isEqualTo("Hello");
    }

    @Test
    void findPublishedBySlugThrowsWhenMissing() {
        when(postRepository.findBySlugAndStatus("missing", PostStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.findPublishedBySlug("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(
                                    rse.getStatusCode().value())
                            .isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }
}
