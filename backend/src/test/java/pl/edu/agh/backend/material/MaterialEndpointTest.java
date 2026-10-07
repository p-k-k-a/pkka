package pl.edu.agh.backend.material;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class MaterialEndpointTest {

    private static final String ADMIN_SUBJECT = "33333333-3333-3333-3333-333333333333";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanMaterials() {
        materialRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void verifiedAlumniCanListMaterialsWithFilters() throws Exception {
        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nagranie","type":"RECORDING","url":"https://example.com/a"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("RECORDING"));

        mockMvc.perform(get("/api/alumni/materials")
                        .param("type", "PRESENTATION")
                        .with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void rejectsMaterialWithNonHttpUrl() throws Exception {
        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Zły link","type":"OTHER","url":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifiedAlumniSeeMaterialsLinkedToAllAlumniEvents() throws Exception {
        String eventBody = mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestBody("ALL_ALUMNI")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(eventBody, "$.id"));

        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nagranie","type":"RECORDING","url":"https://example.com/a","eventId":"%s"}
                                """.formatted(eventId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(eventId.toString()));
    }

    @Test
    void hidesMaterialLinkedToSpecificGroupEventFromAlumniWithoutAMatchingGroup() throws Exception {
        // /api/alumni/** already requires ROLE_VERIFIED_ALUMN, but that role alone doesn't
        // belong to any group yet (see EventVisibility: "SPECIFIC_GROUP is nobody's until
        // groups exist"), so a linked SPECIFIC_GROUP event's material must stay hidden even
        // from an otherwise-verified alumn, mirroring how the event itself would be hidden.
        String eventBody = mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestBody("SPECIFIC_GROUP")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(eventBody, "$.id"));

        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nagranie grupowe","type":"RECORDING","url":"https://example.com/a","eventId":"%s"}
                                """.formatted(eventId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void anonymousAndBasicUsersCannotAccessAlumniMaterials() throws Exception {
        mockMvc.perform(get("/api/alumni/materials")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void materialSurvivesLinkedEventBeingSoftDeleted() throws Exception {
        String eventBody = mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestBody("PUBLIC")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(eventBody, "$.id"));

        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nagranie","type":"RECORDING","url":"https://example.com/a","eventId":"%s"}
                                """.formatted(eventId)))
                .andExpect(status().isCreated());

        // The class-level @Transactional keeps this whole test in one Hibernate session, unlike
        // production where the event deletion and the later material list are separate
        // requests with independent persistence contexts. Clearing the session here detaches
        // the Material/Event instances created above so the delete below doesn't trip over a
        // stale in-memory association — it only reproduces a test-only quirk of sharing one
        // session across what would normally be unrelated requests, not a real bug.
        entityManager.clear();

        mockMvc.perform(delete("/api/admin/events/{id}", eventId)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // Before @NotFound(action = IGNORE) on Material.event, this call threw
        // EntityNotFoundException while resolving the now-invisible (soft-deleted) event and
        // surfaced as an unhandled 500.
        mockMvc.perform(get("/api/alumni/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").doesNotExist());
    }

    @Test
    void searchMatchesTitleDescriptionAndLinkedEventTitleCaseInsensitively() throws Exception {
        String eventBody = mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestBody("PUBLIC")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(eventBody, "$.id"));

        createMaterial("""
                {"title":"Kubernetes w praktyce","type":"RECORDING","url":"https://example.com/a"}
                """);
        createMaterial("""
                {"title":"Slajdy","description":"Wstęp do Kubernetesa","type":"PRESENTATION","url":"https://example.com/b"}
                """);
        createMaterial("""
                {"title":"Nagranie","type":"RECORDING","url":"https://example.com/c","eventId":"%s"}
                """.formatted(eventId));
        createMaterial("""
                {"title":"Inne","type":"OTHER","url":"https://example.com/d"}
                """);

        mockMvc.perform(get("/api/alumni/materials").param("q", "  KUBERNETES ").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // "Wydarzenie" is the linked event's title, not the material's.
        mockMvc.perform(get("/api/alumni/materials").param("q", "wydarz").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Nagranie"));

        // Combines with the other filters.
        mockMvc.perform(get("/api/alumni/materials")
                        .param("q", "kubernetes")
                        .param("type", "PRESENTATION")
                        .with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Slajdy"));

        mockMvc.perform(get("/api/admin/materials")
                        .param("q", "kubernetes")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void searchTreatsLikeWildcardsLiterally() throws Exception {
        createMaterial("""
                {"title":"Nagranie","type":"RECORDING","url":"https://example.com/a"}
                """);

        mockMvc.perform(get("/api/alumni/materials").param("q", "%").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        mockMvc.perform(get("/api/alumni/materials").param("q", "N_granie").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private void createMaterial(String body) throws Exception {
        mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private static String eventRequestBody(String audience) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        return """
                {"title":"Wydarzenie","fullDescription":"Opis","type":"ONLINE","startsAt":"%s","endsAt":"%s","transmissionUrl":"https://meet.example.com","audience":"%s","tags":[]}
                """.formatted(start, end, audience);
    }
}
