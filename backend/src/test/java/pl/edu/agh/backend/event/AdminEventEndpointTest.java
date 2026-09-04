package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminEventEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String ADMIN_SUBJECT = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanEvents() {
        eventRepository.deleteAll();
    }

    private String createEvent(String body) throws Exception {
        return mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String eventJson(String title, String startsAt, String endsAt, String audience, String tags) {
        return """
                {
                  "title": "%s",
                  "shortDescription": "Krótki opis wydarzenia",
                  "fullDescription": "Opis wydarzenia",
                  "type": "ONLINE",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "transmissionUrl": "https://meet.example.com/event",
                  "audience": "%s",
                  "tags": %s
                }
                """.formatted(title, startsAt, endsAt, audience, tags);
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/events").with(JwtTestSupport.asUser())).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asVerifiedAlumn())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(
                                "x",
                                Instant.now().plus(2, ChronoUnit.DAYS).toString(),
                                Instant.now()
                                        .plus(2, ChronoUnit.DAYS)
                                        .plus(2, ChronoUnit.HOURS)
                                        .toString(),
                                "PUBLIC",
                                "[]")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsEventWithTagsAndReturnsAdminPayload() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String body = createEvent(eventJson("Warsztat admina", start.toString(), end.toString(), "PUBLIC", "[\"ai\"]"));
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/admin/events/{id}", id).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Warsztat admina"))
                .andExpect(jsonPath("$.type").value("ONLINE"))
                .andExpect(jsonPath("$.audience").value("PUBLIC"))
                .andExpect(jsonPath("$.transmissionUrl").value("https://meet.example.com/event"))
                .andExpect(jsonPath("$.tags").value(hasItem("ai")));
    }

    @Test
    void exposesAuthorDisplayNameOnDetailAndList() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Z podpisem", start.toString(), end.toString(), "PUBLIC", "[]"));
        String id = JsonPath.read(created, "$.id");

        User author = userRepository.findByKeycloakId(ADMIN_SUBJECT).orElseThrow();
        author.setFirstName("Anna");
        author.setLastName("Nowak");
        userRepository.saveAndFlush(author);

        mockMvc.perform(get("/api/admin/events/{id}", id).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorDisplayName").value("Anna Nowak"));

        mockMvc.perform(get("/api/admin/events").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorDisplayName").value("Anna Nowak"));
    }

    /** Blank lines around embedded images are layout, so the description must survive verbatim. */
    @Test
    void keepsBlankLinesInsideDescription() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent("""
                {
                  "title": "Z plakatem",
                  "fullDescription": "Zapraszamy!\\n\\n\\n![plakat](https://example.com/plakat.png)\\n\\n",
                  "type": "ONLINE",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "audience": "PUBLIC",
                  "tags": []
                }
                """.formatted(start, end));

        assertThat(JsonPath.<String>read(created, "$.fullDescription"))
                .isEqualTo("Zapraszamy!\n\n\n![plakat](https://example.com/plakat.png)\n\n");
    }

    @Test
    void rejectsBlankTitleAndInvertedPeriod() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("  ", start.toString(), end.toString(), "PUBLIC", "[]")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson("Złe daty", end.toString(), start.toString(), "PUBLIC", "[]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownTags() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(
                                "Nieznany tag", start.toString(), end.toString(), "PUBLIC", "[\"does-not-exist\"]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsPastAndUpcomingAndAlumniOnlyEvents() throws Exception {
        Instant upcomingStart = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant upcomingEnd = upcomingStart.plus(2, ChronoUnit.HOURS);
        Instant pastStart = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant pastEnd = pastStart.plus(2, ChronoUnit.HOURS);

        createEvent(eventJson("Nadchodzące", upcomingStart.toString(), upcomingEnd.toString(), "ALL_ALUMNI", "[]"));
        createEvent(eventJson("Minione", pastStart.toString(), pastEnd.toString(), "PUBLIC", "[]"));

        mockMvc.perform(get("/api/admin/events").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/admin/events").param("timeframe", "UPCOMING").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Nadchodzące"))
                .andExpect(jsonPath("$.content[0].audience").value("ALL_ALUMNI"));

        mockMvc.perform(get("/api/admin/events").param("timeframe", "PAST").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Minione"));
    }

    @Test
    void updatesEventFields() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Wersja 1", start.toString(), end.toString(), "PUBLIC", "[]"));
        String id = JsonPath.read(created, "$.id");

        Instant newStart = start.plus(1, ChronoUnit.DAYS);
        Instant newEnd = newStart.plus(3, ChronoUnit.HOURS);
        mockMvc.perform(put("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Wersja 2",
                                  "shortDescription": "Nowy krótki opis",
                                  "fullDescription": "Nowy opis",
                                  "type": "HYBRID",
                                  "startsAt": "%s",
                                  "endsAt": "%s",
                                  "location": "AGH D-17",
                                  "seatLimit": 80,
                                  "audience": "ALL_ALUMNI",
                                  "tags": ["ai", "alumni"]
                                }
                                """.formatted(newStart, newEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Wersja 2"))
                .andExpect(jsonPath("$.shortDescription").value("Nowy krótki opis"))
                .andExpect(jsonPath("$.type").value("HYBRID"))
                .andExpect(jsonPath("$.location").value("AGH D-17"))
                .andExpect(jsonPath("$.seatLimit").value(80))
                .andExpect(jsonPath("$.audience").value("ALL_ALUMNI"))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void getReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/admin/events/{id}", UUID.randomUUID()).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesEventAndHidesItFromPublicApi() throws Exception {
        Instant start = Instant.now().plus(6, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Do kasacji", start.toString(), end.toString(), "PUBLIC", "[]"));
        String id = JsonPath.read(created, "$.id");

        mockMvc.perform(delete("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/events/{id}", id).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/events/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanListEventTags() throws Exception {
        mockMvc.perform(get("/api/tags").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }
}
