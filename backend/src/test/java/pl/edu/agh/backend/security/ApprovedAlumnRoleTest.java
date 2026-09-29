package pl.edu.agh.backend.security;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
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
class ApprovedAlumnRoleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");

    @Test
    void bearerTokenIssuedBeforeApprovalReachesTheAlumniDirectory() throws Exception {
        applicant("kc-approved-bearer", true);

        mockMvc.perform(get("/api/alumni")
                        .with(jwt().jwt(j -> j.subject("kc-approved-bearer")).authorities(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void sessionCreatedBeforeApprovalReachesTheAlumniDirectory() throws Exception {
        applicant("kc-approved-session", true);

        mockMvc.perform(get("/api/alumni")
                        .with(oidcLogin()
                                .idToken(t -> t.subject("kc-approved-session"))
                                .authorities(USER)))
                .andExpect(status().isOk());
    }

    @Test
    void meReportsTheApprovalWithoutANewToken() throws Exception {
        applicant("kc-approved-me", true);

        mockMvc.perform(get("/api/me")
                        .with(jwt().jwt(j -> j.subject("kc-approved-me")).authorities(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem(Roles.VERIFIED_ALUMN)));
    }

    @Test
    void pendingApplicantStaysOutOfTheAlumniDirectory() throws Exception {
        applicant("kc-pending", false);

        mockMvc.perform(get("/api/alumni")
                        .with(jwt().jwt(j -> j.subject("kc-pending")).authorities(USER)))
                .andExpect(status().isForbidden());
    }

    private void applicant(String keycloakId, boolean approved) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user = userRepository.save(user);
        Application application = Application.builder()
                .applicant(user)
                .status(ApplicationStatus.UNDER_REVIEW)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .build();
        if (approved) {
            application.approve(user);
        }
        applicationRepository.save(application);
    }
}
