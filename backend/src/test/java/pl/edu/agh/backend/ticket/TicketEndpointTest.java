package pl.edu.agh.backend.ticket;

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
class TicketEndpointTest {

    private static final String ALUMN_SUBJECT = "44444444-4444-4444-4444-444444444444";
    private static final String OTHER_ALUMN_SUBJECT = "66666666-6666-6666-6666-666666666666";
    private static final String ADMIN_SUBJECT = "55555555-5555-5555-5555-555555555555";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void cleanTickets() {
        ticketRepository.deleteAll();
    }

    private UUID submit(String subject, String category, String title) throws Exception {
        String body = mockMvc.perform(post("/api/alumni/tickets")
                        .with(JwtTestSupport.asVerifiedAlumn(subject))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"%s","title":"%s","description":"Opis zgłoszenia"}
                                """.formatted(category, title)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.category").value(category))
                .andExpect(jsonPath("$.adminResponse").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    @Test
    void unknownSortPropertyIsABadRequest() throws Exception {
        // the alumn list goes through a derived query, the admin list through a Specification;
        // the alumn needs a ticket first, or there's no user row and no query runs at all
        submit(ALUMN_SUBJECT, "OTHER", "Cokolwiek");
        mockMvc.perform(get("/api/alumni/tickets")
                        .param("sort", "bogus,desc")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort"));
        mockMvc.perform(get("/api/admin/tickets").param("sort", "bogus").with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort"));
    }

    @Test
    void alumnSubmitsAndSeesAdminReply() throws Exception {
        UUID id = submit(ALUMN_SUBJECT, "TOPIC_PROPOSAL", "Temat AI");

        mockMvc.perform(get("/api/admin/tickets/{id}", id).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Temat AI"))
                .andExpect(jsonPath("$.authorDisplayName").value("Alumn"));

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","adminResponse":"Dodamy to na najbliższe spotkanie"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        mockMvc.perform(get("/api/alumni/tickets/{id}", id).with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResponse").value("Dodamy to na najbliższe spotkanie"));

        mockMvc.perform(get("/api/alumni/tickets").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()));
    }

    @Test
    void statusOnlyPatchKeepsExistingAdminResponse() throws Exception {
        UUID id = submit(ALUMN_SUBJECT, "TECHNICAL_ISSUE", "Nie działa logowanie");

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS","adminResponse":"Sprawdzamy"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResponse").value("Sprawdzamy"));
    }

    @Test
    void adminFiltersByStatusAndCategory() throws Exception {
        UUID topic = submit(ALUMN_SUBJECT, "TOPIC_PROPOSAL", "Temat");
        submit(ALUMN_SUBJECT, "TECHNICAL_ISSUE", "Błąd");

        mockMvc.perform(get("/api/admin/tickets")
                        .param("category", "TOPIC_PROPOSAL")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(topic.toString()));

        mockMvc.perform(get("/api/admin/tickets")
                        .param("status", "RESOLVED")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/admin/tickets").with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void alumnCannotSeeAnotherAlumnsTicket() throws Exception {
        UUID id = submit(ALUMN_SUBJECT, "OTHER", "Prywatne");
        submit(OTHER_ALUMN_SUBJECT, "OTHER", "Moje");

        mockMvc.perform(get("/api/alumni/tickets/{id}", id).with(JwtTestSupport.asVerifiedAlumn(OTHER_ALUMN_SUBJECT)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/alumni/tickets").with(JwtTestSupport.asVerifiedAlumn(OTHER_ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Moje"));
    }

    @Test
    void listMyTicketsReturnsEmptyPageForVerifiedAlumnWithoutLocalUserRow() throws Exception {
        mockMvc.perform(get("/api/alumni/tickets")
                        .with(JwtTestSupport.asVerifiedAlumn(UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsInvalidTickets() throws Exception {
        mockMvc.perform(post("/api/alumni/tickets")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Bez kategorii","description":"Opis"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/alumni/tickets")
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"OTHER","title":"Temat","description":"%s"}
                                """.formatted("a".repeat(5001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPatchWithoutStatus() throws Exception {
        UUID id = submit(ALUMN_SUBJECT, "OTHER", "Temat");

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesRoles() throws Exception {
        mockMvc.perform(get("/api/alumni/tickets")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/alumni/tickets").with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/tickets").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownTicketIs404ForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/tickets/{id}", UUID.randomUUID()).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isNotFound());
    }
}
