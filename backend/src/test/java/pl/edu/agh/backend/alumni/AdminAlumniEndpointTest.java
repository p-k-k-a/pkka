package pl.edu.agh.backend.alumni;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.application.Faculty;
import pl.edu.agh.backend.application.StudyType;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminAlumniEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EntityManager entityManager;

    private String marker;
    private User hidden;

    @BeforeEach
    void setUp() {
        marker = UUID.randomUUID().toString().substring(0, 8);
        hidden = new User();
        hidden.setKeycloakId(UUID.randomUUID().toString());
        hidden.setFirstName("Zdzisław" + marker);
        hidden.setLastName("Nowak");
        hidden.setEmail("zdzislaw." + marker + "@example.com");
        hidden.setDiscordId("123456789012345678");
        hidden.setShowName(false);
        hidden.setShowEmail(false);
        hidden.setShowDiscord(false);
        hidden = userRepository.save(hidden);
        approve(hidden);
        entityManager.flush();
        entityManager.clear();
    }

    private void approve(User applicant) {
        applicationRepository.save(Application.builder()
                .applicant(applicant)
                .status(ApplicationStatus.APPROVED)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .reviewedAt(Instant.parse("2021-06-15T10:00:00Z"))
                .build());
    }

    // Real tokens always carry USER too; without it the catch-all USER rule would 403 these on its own.
    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor verifiedAlumn() {
        return jwt().authorities(
                        new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
    }

    @Test
    void adminListFindsAlumniHidingTheirNameAndShowsTheirSettings() throws Exception {
        mockMvc.perform(get("/api/admin/alumni").param("q", "Zdzisław" + marker).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(hidden.getId().toString()))
                .andExpect(jsonPath("$.content[0].firstName").value("Zdzisław" + marker))
                .andExpect(jsonPath("$.content[0].lastName").value("Nowak"))
                .andExpect(jsonPath("$.content[0].visibility.name").value(false))
                .andExpect(jsonPath("$.content[0].visibility.email").value(false))
                .andExpect(jsonPath("$.content[0].visibility.discord").value(false));
    }

    @Test
    void adminProfileShowsHiddenFieldsAndTheRealSettings() throws Exception {
        mockMvc.perform(get("/api/admin/alumni/{id}", hidden.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Zdzisław" + marker))
                .andExpect(jsonPath("$.lastName").value("Nowak"))
                .andExpect(jsonPath("$.email").value("zdzislaw." + marker + "@example.com"))
                .andExpect(jsonPath("$.discordId").value("123456789012345678"))
                .andExpect(jsonPath("$.visibility.name").value(false))
                .andExpect(jsonPath("$.visibility.email").value(false))
                .andExpect(jsonPath("$.visibility.discord").value(false));
    }

    @Test
    void verifiedAlumnCannotUseTheAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/alumni").with(verifiedAlumn())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/alumni/{id}", hidden.getId()).with(verifiedAlumn()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutAlumnRoleCannotUseTheAlumniEndpoints() throws Exception {
        mockMvc.perform(get("/api/alumni").with(admin())).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/alumni/{id}", hidden.getId()).with(admin())).andExpect(status().isForbidden());
    }
}
