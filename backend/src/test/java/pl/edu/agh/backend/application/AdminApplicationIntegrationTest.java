package pl.edu.agh.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleService;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @MockitoBean
    private KeycloakRoleService keycloakRoleService;

    private User applicant;
    private User admin;
    private Application pendingApplication;

    @BeforeEach
    void setUp() {
        applicant = new User();
        applicant.setKeycloakId(UUID.randomUUID().toString());
        applicant.setEmail("applicant@example.com");
        applicant = userRepository.save(applicant);

        admin = new User();
        admin.setKeycloakId(UUID.randomUUID().toString());
        admin = userRepository.save(admin);

        pendingApplication = Application.builder()
                .applicant(applicant)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2019)
                .phoneNumber("+48111222333")
                .build();
        pendingApplication.addConsent(ConsentType.REGULATIONS_PRIVACY, java.time.Instant.now());
        pendingApplication.addConsent(ConsentType.GDPR_DATA_PROCESSING, java.time.Instant.now());
        pendingApplication = applicationRepository.save(pendingApplication);
    }

    @Test
    void listUnderReviewApplications() throws Exception {
        mockMvc.perform(get("/api/admin/applications").with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.application.id=='%s')]".formatted(pendingApplication.getId()))
                        .exists());
    }

    @Test
    void getApplicationById() throws Exception {
        mockMvc.perform(get("/api/admin/applications/{id}", pendingApplication.getId())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.id")
                        .value(pendingApplication.getId().toString()))
                .andExpect(jsonPath("$.applicantKeycloakId").value(applicant.getKeycloakId()))
                .andExpect(jsonPath("$.application.status").value("UNDER_REVIEW"));
    }

    @Test
    void getUnknownApplicationReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/applications/{id}", UUID.randomUUID())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveApplicationUpdatesStatusAndGraduationYear() throws Exception {
        mockMvc.perform(post("/api/admin/applications/{id}/approve", pendingApplication.getId())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        User updatedApplicant = userRepository.findById(applicant.getId()).orElseThrow();
        assertThat(updatedApplicant.getGraduationYear()).isEqualTo(2019);
    }

    @Test
    void rejectApplicationWithReason() throws Exception {
        mockMvc.perform(post("/api/admin/applications/{id}/reject", pendingApplication.getId())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Missing proof of graduation\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Missing proof of graduation"));
    }

    @Test
    void approveAlreadyApprovedReturns409() throws Exception {
        pendingApplication.approve(admin);
        applicationRepository.saveAndFlush(pendingApplication);

        mockMvc.perform(post("/api/admin/applications/{id}/approve", pendingApplication.getId())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectAlreadyRejectedReturns409() throws Exception {
        pendingApplication.reject(admin, "no");
        applicationRepository.saveAndFlush(pendingApplication);

        mockMvc.perform(post("/api/admin/applications/{id}/reject", pendingApplication.getId())
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/applications").with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/applications/{id}/approve", pendingApplication.getId())
                        .with(JwtTestSupport.asUser(applicant.getKeycloakId()))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAdminRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/applications")).andExpect(status().isUnauthorized());
    }
}
