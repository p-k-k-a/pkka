package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
                  "fullDescription": "Opis wydarzenia",
                  "type": "ONLINE",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "transmissionUrl": "https://meet.google.com/abc-defg-hij",
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
        String slug = JsonPath.read(body, "$.slug");

        mockMvc.perform(get("/api/admin/events/by-slug/{slug}", slug).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("warsztat-admina"))
                .andExpect(jsonPath("$.title").value("Warsztat admina"))
                .andExpect(jsonPath("$.type").value("ONLINE"))
                .andExpect(jsonPath("$.audience").value("PUBLIC"))
                .andExpect(jsonPath("$.transmissionUrl").value("https://meet.google.com/abc-defg-hij"))
                .andExpect(jsonPath("$.tags").value(hasItem("ai")));
    }

    @Test
    void exposesAuthorDisplayNameOnDetailAndList() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Z podpisem", start.toString(), end.toString(), "PUBLIC", "[]"));
        String slug = JsonPath.read(created, "$.slug");

        User author = userRepository.findByKeycloakId(ADMIN_SUBJECT).orElseThrow();
        author.setFirstName("Anna");
        author.setLastName("Nowak");
        userRepository.saveAndFlush(author);

        mockMvc.perform(get("/api/admin/events/by-slug/{slug}", slug).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorDisplayName").value("Anna Nowak"));

        mockMvc.perform(get("/api/admin/events").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorDisplayName").value("Anna Nowak"))
                .andExpect(jsonPath("$.content[0].slug").value("z-podpisem"));
    }

    @Test
    void generatesSlugsFromTitlesAndDeduplicatesThem() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String json = eventJson("Wieczór planszówek", start.toString(), end.toString(), "PUBLIC", "[]");

        assertThat(JsonPath.<String>read(createEvent(json), "$.slug")).isEqualTo("wieczor-planszowek");
        assertThat(JsonPath.<String>read(createEvent(json), "$.slug")).isEqualTo("wieczor-planszowek-2");
        assertThat(JsonPath.<String>read(createEvent(json), "$.slug")).isEqualTo("wieczor-planszowek-3");
    }

    @Test
    void reusesTheSlugOfADeletedEvent() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String json = eventJson("Gala", start.toString(), end.toString(), "PUBLIC", "[]");
        String id = JsonPath.read(createEvent(json), "$.id");

        mockMvc.perform(delete("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(JsonPath.<String>read(createEvent(json), "$.slug")).isEqualTo("gala");
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

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc%40thread.v2/0",
                "https://teams.live.com/meet/9876543210",
                "https://us02web.zoom.us/j/12345678901?pwd=abc",
                "https://univienna.zoom.us/j/12345678901",
                "https://zoom.us/my/jan.kowalski",
                "https://zoom.us/w/12345678901",
                "https://zoom.com/j/12345678901",
                "https://meet.google.com/abc-defg-hij"
            })
    void acceptsTeamsZoomAndMeetLinks(String transmissionUrl) throws Exception {
        String created = createEvent(onlineEventJson(Instant.now().plus(3, ChronoUnit.DAYS), transmissionUrl));

        assertThat(JsonPath.<String>read(created, "$.transmissionUrl")).isEqualTo(transmissionUrl);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://meet.example.com/event",
                "http://meet.google.com/abc-defg-hij",
                "https://meet.google.com.example.com/abc-defg-hij",
                "https://zoom.us.example.com/j/12345678901",
                "https://zoom.com.example.com/j/12345678901",
                "javascript:alert(1)"
            })
    void rejectsLinksOutsideTeamsZoomAndMeet(String transmissionUrl) throws Exception {
        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(onlineEventJson(Instant.now().plus(3, ChronoUnit.DAYS), transmissionUrl)))
                .andExpect(status().isBadRequest());
    }

    private static String onlineEventJson(Instant start, String transmissionUrl) {
        return """
                {
                  "title": "Spotkanie online",
                  "type": "ONLINE",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "transmissionUrl": "%s",
                  "audience": "PUBLIC",
                  "tags": []
                }
                """.formatted(start, start.plus(2, ChronoUnit.HOURS), transmissionUrl);
    }

    @Test
    void acceptsAReminderUpToAWeekBeforeTheStart() throws Exception {
        Instant start = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String reminderJson = """
                {
                  "title": "Przypomnienie",
                  "type": "ONLINE",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "audience": "PUBLIC",
                  "reminderLeadTimeMinutes": %d,
                  "tags": []
                }
                """;

        createEvent(reminderJson.formatted(start, end, 7 * 24 * 60));

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reminderJson.formatted(start, end, 7 * 24 * 60 + 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createsUnknownTagsLowerCasedAndListsThemInTheCatalog() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        String body = createEvent(
                eventJson("Nowy tag", start.toString(), end.toString(), "PUBLIC", "[\" Hackathon \", \"AI\"]"));

        List<String> tags = JsonPath.read(body, "$.tags");
        assertThat(tags).containsExactlyInAnyOrder("hackathon", "ai");
        mockMvc.perform(get("/api/tags").with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[?(@.name=='hackathon')]").value(hasSize(1)))
                .andExpect(jsonPath("$[?(@.name=='ai')]").value(hasSize(1)));
    }

    @Test
    void rejectsTagNamesLongerThan32Characters() throws Exception {
        Instant start = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventJson(
                                "Zły tag",
                                start.toString(),
                                end.toString(),
                                "PUBLIC",
                                "[\"%s\"]".formatted("x".repeat(33)))))
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
    void listsAnEventUnderWayAsUpcomingUntilItEnds() throws Exception {
        Instant start = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(30, ChronoUnit.MINUTES);
        createEvent(eventJson("Trwające", start.toString(), end.toString(), "PUBLIC", "[]"));

        mockMvc.perform(get("/api/admin/events").param("timeframe", "UPCOMING").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Trwające"));

        mockMvc.perform(get("/api/admin/events").param("timeframe", "PAST").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updatesEventFields() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Wersja 1", start.toString(), end.toString(), "PUBLIC", "[]"));
        String id = JsonPath.read(created, "$.id");
        assertThat(JsonPath.<String>read(created, "$.slug")).isEqualTo("wersja-1");

        Instant newStart = start.plus(1, ChronoUnit.DAYS);
        Instant newEnd = newStart.plus(3, ChronoUnit.HOURS);
        mockMvc.perform(put("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Wersja 2",
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
                .andExpect(jsonPath("$.slug").value("wersja-1"))
                .andExpect(jsonPath("$.fullDescription").value("Nowy opis"))
                .andExpect(jsonPath("$.type").value("HYBRID"))
                .andExpect(jsonPath("$.location").value("AGH D-17"))
                .andExpect(jsonPath("$.seatLimit").value(80))
                .andExpect(jsonPath("$.audience").value("ALL_ALUMNI"))
                .andExpect(jsonPath("$.tags.length()").value(2));
    }

    @Test
    void getReturns404ForUnknownSlug() throws Exception {
        mockMvc.perform(get("/api/admin/events/by-slug/{slug}", "nie-ma-takiego")
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesEventAndHidesItFromPublicApi() throws Exception {
        Instant start = Instant.now().plus(6, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        String created = createEvent(eventJson("Do kasacji", start.toString(), end.toString(), "PUBLIC", "[]"));
        String id = JsonPath.read(created, "$.id");
        String slug = JsonPath.read(created, "$.slug");

        mockMvc.perform(delete("/api/admin/events/{id}", id)
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/events/by-slug/{slug}", slug).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/events/{slug}", slug)).andExpect(status().isNotFound());
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
