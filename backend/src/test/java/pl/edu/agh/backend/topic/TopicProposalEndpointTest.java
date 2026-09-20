package pl.edu.agh.backend.topic;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class TopicProposalEndpointTest {

    private static final String ALUMN_SUBJECT = "44444444-4444-4444-4444-444444444444";
    private static final String ADMIN_SUBJECT = "55555555-5555-5555-5555-555555555555";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TopicProposalRepository topicProposalRepository;

    @BeforeEach
    void cleanProposals() {
        topicProposalRepository.deleteAll();
    }

    @Test
    void alumniSubmitAndAdminModerates() throws Exception {
        String created = mockMvc.perform(post("/api/alumni/topic-proposals")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Temat AI","description":"Opis tematu","rationale":"Bo warto"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/alumni/topic-proposals").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Temat AI"));

        mockMvc.perform(get("/api/admin/topic-proposals").with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));

        mockMvc.perform(patch("/api/admin/topic-proposals/{id}/status", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void rejectsStatusUpdateWithoutStatusField() throws Exception {
        String created = mockMvc.perform(post("/api/alumni/topic-proposals")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Temat","description":"Opis","rationale":"Bo warto"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(patch("/api/admin/topic-proposals/{id}/status", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMyTopicProposalsReturnsEmptyPageForVerifiedAlumnWithoutLocalUserRow() throws Exception {
        // No local User row exists yet for this subject: CallerUserService only provisions one
        // lazily on the first write. Security already accepted the token; zero proposals => 200 [].
        mockMvc.perform(get("/api/alumni/topic-proposals")
                        .with(JwtTestSupport.asVerifiedAlumn(UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void authorDisplayNameFallsBackInsteadOfRenderingNullNullForAFreshlyProvisionedUser() throws Exception {
        // The author's local User row is auto-provisioned on first submit and has no profile
        // (first/last name) yet — see User.getDisplayName().
        mockMvc.perform(post("/api/alumni/topic-proposals")
                        .with(JwtTestSupport.asVerifiedAlumn(UUID.randomUUID().toString()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Temat","description":"Opis","rationale":"Bo warto"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorDisplayName").value("Alumn"));
    }

    @Test
    void rejectsProposalWithOversizedDescription() throws Exception {
        String tooLong = "a".repeat(5001);
        mockMvc.perform(post("/api/alumni/topic-proposals")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Temat","description":"%s","rationale":"Bo warto"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest());
    }
}
