package pl.edu.agh.backend.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import pl.edu.agh.backend.user.UserTag;
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

    @BeforeEach
    void setUp() {
        user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName("Anna");
        user.setBio("Bio");
        user = userRepository.save(user);

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
                        .content("{\"names\": [\"Java\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private void putTags(User owner, String body) throws Exception {
        mockMvc.perform(put("/api/profiles/me/tags")
                        .with(JwtTestSupport.asUser(owner.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private User anotherApprovedAlumnus(boolean showName) {
        User other = new User();
        other.setKeycloakId(UUID.randomUUID().toString());
        other.setFirstName("Jan");
        other.setShowName(showName);
        other = userRepository.save(other);
        applicationRepository.save(Application.builder()
                .applicant(other)
                .status(ApplicationStatus.APPROVED)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .reviewedAt(java.time.Instant.parse("2022-01-01T00:00:00Z"))
                .build());
        return other;
    }

    private List<String> catalogNames() {
        return userTagRepository.findAll().stream().map(UserTag::getName).toList();
    }

    @Test
    void updateMyTagsCreatesUnknownNamesKeepingTheSpellingAsTyped() throws Exception {
        putTags(user, "{\"names\": [\"  Elixir   OTP \"]}");

        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Elixir OTP"));
        assertThat(catalogNames()).contains("Elixir OTP");
    }

    @Test
    void updateMyTagsMatchesExistingNamesIgnoringCase() throws Exception {
        long before = userTagRepository.count();

        putTags(user, "{\"names\": [\"jAVA\", \"java\"]}");

        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Java"));
        assertThat(userTagRepository.count()).isEqualTo(before);
    }

    @Test
    void updateMyTagsRejectsBlankAndTooLongNames() throws Exception {
        for (String name : List.of("\"   \"", "\"%s\"".formatted("x".repeat(33)), "null")) {
            mockMvc.perform(put("/api/profiles/me/tags")
                            .with(JwtTestSupport.asUser(user.getKeycloakId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"names\": [%s]}".formatted(name)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(catalogNames()).doesNotContain("x".repeat(33));
    }

    @Test
    void updateMyTagsAcceptsPunctuationUsedByTechnologyNames() throws Exception {
        putTags(user, "{\"names\": [\"C++\", \"C#\", \"Node.js\", \"CI/CD\", \"Zażółć-2\", \"R\"]}");

        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void updateMyTagsRejectsMoreThanTwentyTags() throws Exception {
        String names = IntStream.rangeClosed(1, 21)
                .mapToObj(i -> "\"skill %d\"".formatted(i))
                .collect(Collectors.joining(","));

        mockMvc.perform(put("/api/profiles/me/tags")
                        .with(JwtTestSupport.asUser(user.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\": [%s]}".formatted(names)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogCountsOnlyAlumniListedInTheDirectory() throws Exception {
        User listed = anotherApprovedAlumnus(true);
        User hidden = anotherApprovedAlumnus(false);
        for (User owner : List.of(user, listed, hidden)) {
            putTags(owner, "{\"names\": [\"Elixir\"]}");
        }

        mockMvc.perform(get("/api/user-tags").with(JwtTestSupport.asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Elixir')].usageCount").value(contains(2)))
                .andExpect(jsonPath("$[?(@.name=='Blockchain')].usageCount").value(contains(0)));
    }

    @Test
    void updateMyTagsRequiresAVerifiedAlumnus() throws Exception {
        User applicant = new User();
        applicant.setKeycloakId(UUID.randomUUID().toString());
        applicant = userRepository.save(applicant);

        mockMvc.perform(put("/api/profiles/me/tags")
                        .with(JwtTestSupport.asUser(applicant.getKeycloakId()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"names\": [\"Applicant Skill\"]}"))
                .andExpect(status().isForbidden());
        assertThat(catalogNames()).doesNotContain("Applicant Skill");
    }

    @Test
    void updateMyTagsRejectsMissingNames() throws Exception {
        putTags(user, "{\"names\": [\"Java\"]}");

        for (String body : List.of("{}", "{\"names\": null}")) {
            mockMvc.perform(put("/api/profiles/me/tags")
                            .with(JwtTestSupport.asUser(user.getKeycloakId()))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/profiles/me/tags").with(JwtTestSupport.asUser(user.getKeycloakId())))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/profiles/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profiles/me/tags")).andExpect(status().isUnauthorized());
    }
}
