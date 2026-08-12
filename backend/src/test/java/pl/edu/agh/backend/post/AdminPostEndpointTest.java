package pl.edu.agh.backend.post;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** Covers the {@code /api/admin/posts} CRUD endpoints: creation with auto-slug, listing drafts, editing, the draft/published toggle and deletion. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(AdminPostEndpointTest.TestSecurityBeans.class)
@Transactional
class AdminPostEndpointTest {

    private static final String ADMIN_SUBJECT = "10000000-0000-0000-0000-000000000001";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanPosts() {
        postRepository.deleteAll();
    }

    private static JwtRequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject(ADMIN_SUBJECT)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private String createPost(String body) throws Exception {
        return mockMvc.perform(post("/api/admin/posts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/posts").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/posts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"content\":\"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsDraftWithSlugGeneratedFromTitle() throws Exception {
        String body = createPost("""
                {"title": "Koło naukowe — spotkanie", "excerpt": "Zapraszamy!", "content": "## Hej"}
                """);

        assertEquals("kolo-naukowe-spotkanie", JsonPath.read(body, "$.slug"));
        assertEquals("DRAFT", JsonPath.read(body, "$.status"));
        assertEquals("Zapraszamy!", JsonPath.read(body, "$.excerpt"));
        assertEquals(ADMIN_SUBJECT, JsonPath.read(body, "$.authorId"));
        Object publishedAt = JsonPath.read(body, "$.publishedAt");
        assertNull(publishedAt);
    }

    @Test
    void deduplicatesSlugsForIdenticalTitles() throws Exception {
        String first = createPost("{\"title\": \"Ten sam tytuł\", \"content\": \"a\"}");
        String second = createPost("{\"title\": \"Ten sam tytuł\", \"content\": \"b\"}");

        assertEquals("ten-sam-tytul", JsonPath.read(first, "$.slug"));
        assertEquals("ten-sam-tytul-2", JsonPath.read(second, "$.slug"));
    }

    @Test
    void createsPublishedPostWithPublishedAt() throws Exception {
        String body = createPost("{\"title\": \"Od razu na żywo\", \"content\": \"c\", \"status\": \"PUBLISHED\"}");

        assertEquals("PUBLISHED", JsonPath.read(body, "$.status"));
        Object publishedAt = JsonPath.read(body, "$.publishedAt");
        assertNotNull(publishedAt);
    }

    @Test
    void rejectsBlankTitleAndContent() throws Exception {
        mockMvc.perform(post("/api/admin/posts")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"  \", \"content\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsDraftsAndPublishedNewestFirstWithStatusFilter() throws Exception {
        createPost("{\"title\": \"Pierwszy\", \"content\": \"a\", \"status\": \"PUBLISHED\"}");
        createPost("{\"title\": \"Drugi\", \"content\": \"b\"}");

        mockMvc.perform(get("/api/admin/posts").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Drugi"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));

        mockMvc.perform(get("/api/admin/posts").param("status", "PUBLISHED").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Pierwszy"));
    }

    @Test
    void draftsStayHiddenFromThePublicApi() throws Exception {
        String body = createPost("{\"title\": \"Szkic\", \"content\": \"tajne\"}");
        String slug = JsonPath.read(body, "$.slug");

        mockMvc.perform(get("/api/public/posts/{slug}", slug)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updatesFieldsAndTogglesPublicationKeepingSlugAndOriginalPublishedAt() throws Exception {
        String body = createPost("{\"title\": \"Wersja robocza\", \"content\": \"v1\"}");
        String id = JsonPath.read(body, "$.id");

        String published = mockMvc.perform(
                        put("/api/admin/posts/{id}", id)
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"title\": \"Nowy tytuł\", \"excerpt\": \"zajawka\", \"content\": \"v2\", \"status\": \"PUBLISHED\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("wersja-robocza", JsonPath.read(published, "$.slug"));
        assertEquals("Nowy tytuł", JsonPath.read(published, "$.title"));
        assertEquals("v2", JsonPath.read(published, "$.content"));
        String publishedAt = JsonPath.read(published, "$.publishedAt");

        String unpublished = mockMvc.perform(
                        put("/api/admin/posts/{id}", id)
                                .with(admin())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"title\": \"Nowy tytuł\", \"excerpt\": \"zajawka\", \"content\": \"v2\", \"status\": \"DRAFT\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals("DRAFT", JsonPath.read(unpublished, "$.status"));
        assertEquals(publishedAt, JsonPath.read(unpublished, "$.publishedAt"));
    }

    @Test
    void getReturnsContentAnd404ForUnknownId() throws Exception {
        String body = createPost("{\"title\": \"Do odczytu\", \"content\": \"## markdown\"}");
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/admin/posts/{id}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("## markdown"));

        mockMvc.perform(get("/api/admin/posts/{id}", UUID.randomUUID()).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesPost() throws Exception {
        String body = createPost("{\"title\": \"Do kasacji\", \"content\": \"x\"}");
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(delete("/api/admin/posts/{id}", id).with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/admin/posts/{id}", id).with(admin())).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/posts/{id}", id).with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void provisionsAuthorRowForFirstTimeAdmin() throws Exception {
        createPost("{\"title\": \"Pierwszy wpis admina\", \"content\": \"x\"}");

        User author = userRepository.findByKeycloakId(ADMIN_SUBJECT).orElseThrow();
        assertEquals(1, postRepository.findAll().size());
        assertEquals(
                author.getId(), postRepository.findAll().getFirst().getAuthor().getId());
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
