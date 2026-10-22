package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import pl.edu.agh.backend.legal.LegalDocumentRepository;
import pl.edu.agh.backend.legal.LegalDocumentType;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.UserRepository;

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    private String validApplicationBody() {
        return applicationBody(currentIds(
                LegalDocumentType.TERMS, LegalDocumentType.PRIVACY_NOTICE, LegalDocumentType.MEMBERSHIP_CONSENT));
    }

    private List<UUID> currentIds(LegalDocumentType... types) {
        return Arrays.stream(types)
                .map(type -> legalDocumentRepository
                        .findCurrent(type, Instant.now())
                        .orElseThrow()
                        .getId())
                .toList();
    }

    private static String applicationBody(Collection<UUID> acceptedDocumentIds) {
        String ids = acceptedDocumentIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(", "));
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
                  "acceptedDocumentIds": [%s]
                }
                """.formatted(ids);
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
                .andExpect(jsonPath("$.fieldOfStudy").value("Informatyka"));

        mockMvc.perform(get("/api/me/consents").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents[*].type")
                        .value(containsInAnyOrder("TERMS", "PRIVACY_NOTICE", "MEMBERSHIP_CONSENT")))
                .andExpect(jsonPath("$.pendingReacceptance").isEmpty());
    }

    @Test
    void optionalNewsletterConsentIsRecordedWithTheApplication() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(currentIds(LegalDocumentType.values()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/me/consents").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(jsonPath("$.consents[*].type").value(hasItem("NEWSLETTER_CONSENT")));
    }

    @Test
    void outdatedDocumentReturns409AndSavesNothing() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        UUID archivalTerms = legalDocumentRepository.findAll().stream()
                .filter(d -> d.getType() == LegalDocumentType.TERMS && d.getVersion() == 1)
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(List.of(
                                archivalTerms,
                                currentIds(LegalDocumentType.PRIVACY_NOTICE).getFirst(),
                                currentIds(LegalDocumentType.MEMBERSHIP_CONSENT).getFirst()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("OUTDATED_DOCUMENT"));

        mockMvc.perform(get("/api/applications/me").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isNotFound());
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
        String keycloakId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/applications/me").with(JwtTestSupport.asUser(keycloakId)))
                .andExpect(status().isNotFound());

        // Reading must not provision, unlike the write paths.
        assertThat(userRepository.findByKeycloakId(keycloakId)).isEmpty();
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
                                  "acceptedDocumentIds": ["%s"]
                                }
                                """.formatted(
                                        currentIds(LegalDocumentType.TERMS).getFirst())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRequiredConsentsReturns400() throws Exception {
        mockMvc.perform(post("/api/applications")
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(currentIds(LegalDocumentType.TERMS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.missing").value(contains("MEMBERSHIP_CONSENT", "PRIVACY_NOTICE")));
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
