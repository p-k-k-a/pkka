package pl.edu.agh.backend.application;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
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
class ApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    private static String validApplicationBody() {
        return """
                {
                  "faculty": "WI",
                  "fieldOfStudy": "Informatyka",
                  "studyType": "MASTER",
                  "graduationYear": 2020,
                  "phoneNumber": "+48123456789",
                  "interests": ["backend"],
                  "meetingPreferences": ["ONLINE"],
                  "coCreationInterest": false,
                  "newsletterSubscription": false,
                  "consents": ["REGULATIONS_PRIVACY", "GDPR_DATA_PROCESSING"]
                }
                """;
    }

    @Test
    void createApplicationReturns201() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplicationBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.fieldOfStudy").value("Informatyka"))
                .andExpect(jsonPath("$.consents.length()").value(2));
    }

    @Test
    void getMineReturnsLatestApplication() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplicationBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications/me").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.graduationYear").value(2020));
    }

    @Test
    void getMineReturns404WhenNoApplication() throws Exception {
        mockMvc.perform(get("/api/applications/me").with(JwtTestSupport.asUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateApplicationReturns409() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplicationBody()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplicationBody()))
                .andExpect(status().isConflict());
    }

    @Test
    void validationFailureReturns400() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faculty": "WI",
                                  "fieldOfStudy": "",
                                  "studyType": "MASTER",
                                  "graduationYear": 2020,
                                  "phoneNumber": "+48123456789",
                                  "consents": ["REGULATIONS_PRIVACY", "GDPR_DATA_PROCESSING"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredConsentsReturns400() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "faculty": "WI",
                                  "fieldOfStudy": "Informatyka",
                                  "studyType": "MASTER",
                                  "graduationYear": 2020,
                                  "phoneNumber": "+48123456789",
                                  "consents": ["REGULATIONS_PRIVACY"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCreateReturns401() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validApplicationBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedGetMineReturns401() throws Exception {
        mockMvc.perform(get("/api/applications/me")).andExpect(status().isUnauthorized());
    }
}
