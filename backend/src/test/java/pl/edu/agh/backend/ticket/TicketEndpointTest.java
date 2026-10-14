package pl.edu.agh.backend.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class TicketEndpointTest {

    private static final String ALUMN_SUBJECT = "44444444-4444-4444-4444-444444444444";
    private static final String OTHER_ALUMN_SUBJECT = "66666666-6666-6666-6666-666666666666";
    private static final String ADMIN_SUBJECT = "55555555-5555-5555-5555-555555555555";
    private static final String OTHER_ADMIN_SUBJECT = "77777777-7777-7777-7777-777777777777";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

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

    /**
     * Real users always have names: the login handler syncs them from the OIDC claims before any token
     * is issued, so tests that look at display names seed them the same way.
     */
    private void seedUser(String subject, String firstName, String lastName) {
        User user = new User();
        user.setKeycloakId(subject);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.saveAndFlush(user);
    }

    private void reply(UUID id, String status, String adminResponse) throws Exception {
        reply(id, ADMIN_SUBJECT, status, adminResponse);
    }

    private void reply(UUID id, String adminSubject, String status, String adminResponse) throws Exception {
        String body = adminResponse == null
                ? "{\"status\":\"%s\"}".formatted(status)
                : "{\"status\":\"%s\",\"adminResponse\":\"%s\"}".formatted(status, adminResponse);
        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(adminSubject))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void alumnSubmitsAndSeesAdminReply() throws Exception {
        seedUser(ALUMN_SUBJECT, "Anna", "Nowak");
        seedUser(ADMIN_SUBJECT, "Jan", "Kowalski");
        UUID id = submit(ALUMN_SUBJECT, "TOPIC_PROPOSAL", "Temat AI");

        mockMvc.perform(get("/api/admin/tickets/{id}", id).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Temat AI"))
                .andExpect(jsonPath("$.authorDisplayName").value("Anna Nowak"))
                .andExpect(jsonPath("$.respondedByDisplayName").doesNotExist());

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","adminResponse":"Dodamy to na najbliższe spotkanie"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.respondedByDisplayName").value("Jan Kowalski"));

        mockMvc.perform(get("/api/alumni/tickets/{id}", id).with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResponse").value("Dodamy to na najbliższe spotkanie"))
                .andExpect(jsonPath("$.respondedByDisplayName").value("Jan Kowalski"));

        mockMvc.perform(get("/api/alumni/tickets").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].respondedByDisplayName").value("Jan Kowalski"));
    }

    @Test
    void statusOnlyPatchKeepsExistingAdminResponseAndResponder() throws Exception {
        seedUser(ADMIN_SUBJECT, "Jan", "Kowalski");
        UUID id = submit(ALUMN_SUBJECT, "TECHNICAL_ISSUE", "Nie działa logowanie");

        reply(id, "IN_PROGRESS", "Sprawdzamy");

        mockMvc.perform(patch("/api/admin/tickets/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminResponse").value("Sprawdzamy"))
                .andExpect(jsonPath("$.respondedByDisplayName").value("Jan Kowalski"));
    }

    @Test
    void laterReplyFromAnotherAdminTakesOverTheAttribution() throws Exception {
        seedUser(ADMIN_SUBJECT, "Jan", "Kowalski");
        seedUser(OTHER_ADMIN_SUBJECT, "Ewa", "Zielińska");
        UUID id = submit(ALUMN_SUBJECT, "OTHER", "Zmiana odpowiedzi");

        reply(id, ADMIN_SUBJECT, "IN_PROGRESS", "Pierwsza odpowiedź");
        reply(id, OTHER_ADMIN_SUBJECT, "RESOLVED", "Poprawiona odpowiedź");

        mockMvc.perform(get("/api/alumni/tickets/{id}", id).with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminResponse").value("Poprawiona odpowiedź"))
                .andExpect(jsonPath("$.respondedByDisplayName").value("Ewa Zielińska"));
    }

    @Test
    void ticketWithoutReplyHasNoResponder() throws Exception {
        UUID id = submit(ALUMN_SUBJECT, "OTHER", "Bez odpowiedzi");
        reply(id, "IN_PROGRESS", null);

        mockMvc.perform(get("/api/alumni/tickets/{id}", id).with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminResponse").doesNotExist())
                .andExpect(jsonPath("$.respondedByDisplayName").doesNotExist());
    }

    @Test
    void listEndpointsRunTheSameNumberOfStatementsRegardlessOfPageSize() throws Exception {
        seedUser(ALUMN_SUBJECT, "Anna", "Nowak");
        seedUser(ADMIN_SUBJECT, "Jan", "Kowalski");
        RequestPostProcessor alumn = JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT);
        RequestPostProcessor admin = JwtTestSupport.asAdmin(ADMIN_SUBJECT);
        statistics().setStatisticsEnabled(true);
        try {
            // a page size well above the row count keeps Spring from adding a COUNT query once the
            // page fills up, so only the loading of the rows is measured
            String alumnList = "/api/alumni/tickets?size=100";
            String adminList = "/api/admin/tickets?size=100";
            answeredTickets(2);
            long alumnWithTwoTickets = statementsFor(alumnList, alumn);
            long adminWithTwoTickets = statementsFor(adminList, admin);
            // guards against the equality below holding vacuously if the instrumentation stopped counting
            assertThat(alumnWithTwoTickets).isPositive();
            assertThat(adminWithTwoTickets).isPositive();

            answeredTickets(8);

            // author and responder are fetch-joined, so the cost must not grow with the number of rows
            assertThat(statementsFor(alumnList, alumn)).isEqualTo(alumnWithTwoTickets);
            assertThat(statementsFor(adminList, admin)).isEqualTo(adminWithTwoTickets);
        } finally {
            statistics().setStatisticsEnabled(false);
        }
    }

    /**
     * Every row gets its own responder and its own author: with one shared user the first lazy load
     * would be cached in the persistence context and hide a query per row.
     */
    private void answeredTickets(int count) throws Exception {
        for (int i = 0; i < count; i++) {
            String responder = UUID.randomUUID().toString();
            seedUser(responder, "Admin", "Nr" + i);
            submit(UUID.randomUUID().toString(), "OTHER", "Cudze zgłoszenie " + i);
            reply(submit(ALUMN_SUBJECT, "OTHER", "Zgłoszenie " + i), responder, "RESOLVED", "Odpowiedź");
        }
    }

    private Statistics statistics() {
        return entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
    }

    /** Runs the request against a cold persistence context and returns how many SQL statements it cost. */
    private long statementsFor(String url, RequestPostProcessor auth) throws Exception {
        entityManager.flush();
        entityManager.clear();
        statistics().clear();
        mockMvc.perform(get(url).with(auth)).andExpect(status().isOk());
        return statistics().getPrepareStatementCount();
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
