package pl.edu.agh.backend.material;

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
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminMaterialEndpointTest {

    private static final String ADMIN_SUBJECT = "22222222-2222-2222-2222-222222222222";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void cleanMaterials() {
        materialRepository.deleteAll();
        eventRepository.deleteAll();
    }

    private String createMaterial(String body) throws Exception {
        return mockMvc.perform(post("/api/admin/materials")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/materials").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isForbidden());
    }

    @Test
    void createsUpdatesAndDeletesMaterial() throws Exception {
        String created = createMaterial("""
                {"title":"Nagranie","description":"Opis","type":"RECORDING","url":"https://example.com/rec"}
                """);
        UUID id = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/admin/materials/{id}", id).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nagranie"))
                .andExpect(jsonPath("$.type").value("RECORDING"));

        mockMvc.perform(put("/api/admin/materials/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Prezentacja","description":"Nowy opis","type":"PRESENTATION","url":"https://example.com/slides"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("PRESENTATION"));

        mockMvc.perform(delete("/api/admin/materials/{id}", id)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/materials/{id}", id).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void linksMaterialToEvent() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        String eventBody = mockMvc.perform(post("/api/admin/events")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Wydarzenie","fullDescription":"Opis","type":"ONLINE","startsAt":"%s","endsAt":"%s","transmissionUrl":"https://meet.example.com","audience":"PUBLIC","tags":[]}
                                """.formatted(start, end)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID eventId = UUID.fromString(JsonPath.read(eventBody, "$.id"));

        createMaterial("""
                {"title":"Materiał","type":"OTHER","url":"https://example.com/file","eventId":"%s"}
                """.formatted(eventId));

        mockMvc.perform(get("/api/admin/materials")
                        .param("eventId", eventId.toString())
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(eventId.toString()));
    }
}
