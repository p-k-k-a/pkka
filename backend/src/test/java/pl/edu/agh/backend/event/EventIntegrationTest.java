package pl.edu.agh.backend.event;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
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
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class EventIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final UUID PUBLIC_EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222201");
    private static final UUID ALUMNI_ONLY_EVENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222202");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TagRepository tagRepository;

    private UUID pastEventId;

    @BeforeEach
    void setUp() {
        Event past = Event.builder()
                .title("Past event " + UUID.randomUUID())
                .type(EventType.ONLINE)
                .startsAt(Instant.parse("2020-01-01T10:00:00Z"))
                .endsAt(Instant.parse("2020-01-01T12:00:00Z"))
                .audience(Audience.PUBLIC)
                .build();
        pastEventId = eventRepository.save(past).getId();
    }

    @Test
    void listUpcomingPublicEventsWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(PUBLIC_EVENT_ID))
                        .exists());
    }

    @Test
    void anonymousUserDoesNotSeeAlumniOnlyEventsInList() throws Exception {
        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(ALUMNI_ONLY_EVENT_ID))
                        .doesNotExist());
    }

    @Test
    void verifiedAlumnSeesAlumniOnlyEvents() throws Exception {
        mockMvc.perform(get("/api/public/events").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(ALUMNI_ONLY_EVENT_ID))
                        .exists());
    }

    @Test
    void pastEventsAreExcludedFromList() throws Exception {
        mockMvc.perform(get("/api/public/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(pastEventId))
                        .doesNotExist());
    }

    @Test
    void getPublicEventByIdWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/events/{id}", PUBLIC_EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.type").exists());
    }

    @Test
    void anonymousUserGets404ForAlumniOnlyEvent() throws Exception {
        mockMvc.perform(get("/api/public/events/{id}", ALUMNI_ONLY_EVENT_ID)).andExpect(status().isNotFound());
    }

    @Test
    void verifiedAlumnCanAccessAlumniOnlyEvent() throws Exception {
        mockMvc.perform(get("/api/public/events/{id}", ALUMNI_ONLY_EVENT_ID).with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audience").value("ALL_ALUMNI"));
    }

    @Test
    void getUnknownEventReturns404() throws Exception {
        mockMvc.perform(get("/api/public/events/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void filterByTagName() throws Exception {
        Tag aiTag = tagRepository.findAll().stream()
                .filter(t -> "ai".equals(t.getName()))
                .findFirst()
                .orElseThrow();

        Event tagged = Event.builder()
                .title("AI meetup " + UUID.randomUUID())
                .type(EventType.ONLINE)
                .startsAt(Instant.now().plusSeconds(86400 * 5))
                .endsAt(Instant.now().plusSeconds(86400 * 5 + 7200))
                .audience(Audience.PUBLIC)
                .tags(Set.of(aiTag))
                .build();
        UUID taggedId = eventRepository.save(tagged).getId();

        mockMvc.perform(get("/api/public/events").param("tags", "ai"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content[?(@.id=='%s')]".formatted(taggedId)).exists());
    }
}
