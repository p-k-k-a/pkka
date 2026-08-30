package pl.edu.agh.backend.user.profile;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTagRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class ProfileTagsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private User user;
    private UUID tagId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName("Anna");
        user.setBio("Bio");
        user = userRepository.save(user);

        tagId = userTagRepository.findAll().getFirst().getId();

        Application approved = Application.builder()
                .applicant(user)
                .status(ApplicationStatus.APPROVED)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2021)
                .phoneNumber("+48123456789")
                .reviewedAt(java.time.Instant.parse("2022-01-01T00:00:00Z"))
                .build();
        applicationRepository.save(approved);
    }

    @Test
    void getMyProfileReturnsEducationFacts() throws Exception {
        mockMvc.perform(get("/api/profiles/me").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.graduationYear").value(2021))
                .andExpect(jsonPath("$.fieldOfStudy").value("Informatyka"));
    }

    @Test
    void getMyTagsReturnsEmptyInitially() throws Exception {
        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updateMyTagsReplacesTagSet() throws Exception {
        mockMvc.perform(put("/api/profiles/me/tags")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\": [\"%s\"]}".formatted(tagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateMyTagsWithInvalidIdReturns400() throws Exception {
        mockMvc.perform(put("/api/profiles/me/tags")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\": [\"%s\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/profiles/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/me/tags")).andExpect(status().isUnauthorized());
    }
}
