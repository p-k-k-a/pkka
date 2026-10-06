package pl.edu.agh.backend.post;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class PostIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private String publishedSlug;
    private String draftSlug;

    @BeforeEach
    void setUp() {
        User author = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User u = new User();
            u.setKeycloakId(UUID.randomUUID().toString());
            return userRepository.save(u);
        });

        Post published = new Post();
        published.setTitle("Published post " + UUID.randomUUID());
        published.setContent("Published content");
        published.setAuthor(author);
        published.publish();
        publishedSlug = postRepository.save(published).getSlug();

        Post draft = new Post();
        draft.setTitle("Draft post " + UUID.randomUUID());
        draft.setContent("Draft content");
        draft.setAuthor(author);
        draftSlug = postRepository.save(draft).getSlug();
    }

    @Test
    void listPublishedPostsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.slug=='%s')]".formatted(publishedSlug))
                        .exists())
                .andExpect(jsonPath("$.content[?(@.slug=='%s')]".formatted(draftSlug))
                        .doesNotExist());
    }

    @Test
    void getPublishedPostBySlug() throws Exception {
        mockMvc.perform(get("/api/public/posts/{slug}", publishedSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(publishedSlug))
                .andExpect(jsonPath("$.content").value("Published content"));
    }

    @Test
    void getDraftPostBySlugReturns404() throws Exception {
        mockMvc.perform(get("/api/public/posts/{slug}", draftSlug)).andExpect(status().isNotFound());
    }

    @Test
    void getUnknownSlugReturns404() throws Exception {
        mockMvc.perform(get("/api/public/posts/{slug}", "nonexistent-slug-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPostsSupportsPagination() throws Exception {
        mockMvc.perform(get("/api/public/posts").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.size").value(1));
    }
}
