package pl.edu.agh.backend.alumni.contact;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class ContactEndpointTest {

    private static final String CONTACTS = "/api/alumni/me/contacts";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    private User owner;
    private User anna;

    @BeforeEach
    void setUp() {
        owner = approvedAlumn("Jan", "Kowalski");
        anna = approvedAlumn("Anna", "Nowak");
        anna.setDiscordId("123456789012345678");
        anna.setCurrentPosition("Backend Developer");
        anna.setCompany("ACME");
        anna.setGraduationYear(2019);
        userRepository.save(anna);
    }

    @Test
    void addsContactAndListsIt() throws Exception {
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONTACTS).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(anna.getId().toString()))
                .andExpect(jsonPath("$[0].firstName").value("Anna"))
                .andExpect(jsonPath("$[0].lastName").value("Nowak"))
                .andExpect(jsonPath("$[0].currentPosition").value("Backend Developer"))
                .andExpect(jsonPath("$[0].company").value("ACME"))
                .andExpect(jsonPath("$[0].graduationYear").value(2019))
                .andExpect(jsonPath("$[0].discordId").value("123456789012345678"))
                .andExpect(jsonPath("$[0].visibility.discord").value(true));
    }

    @Test
    void addingTwiceKeepsOneContact() throws Exception {
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONTACTS).with(as(owner)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsAddingYourself() throws Exception {
        mockMvc.perform(put(CONTACTS + "/{id}", owner.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAddingUserWithoutApprovedApplication() throws Exception {
        User applicant = new User();
        applicant.setKeycloakId(UUID.randomUUID().toString());
        applicant.setFirstName("Piotr");
        applicant = userRepository.save(applicant);

        mockMvc.perform(put(CONTACTS + "/{id}", applicant.getId())
                        .with(as(owner))
                        .with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(CONTACTS + "/{id}", UUID.randomUUID())
                        .with(as(owner))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void removesContact() throws Exception {
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONTACTS).with(as(owner)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void removingSomeoneWhoIsNotAContactSucceeds() throws Exception {
        mockMvc.perform(delete(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void listShowsOnlyTheCallersContacts() throws Exception {
        User other = approvedAlumn("Piotr", "Wiśniewski");
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONTACTS).with(as(other)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void hidesDiscordWhenTheContactHidesIt() throws Exception {
        anna.setShowDiscord(false);
        userRepository.save(anna);
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CONTACTS).with(as(owner)))
                .andExpect(jsonPath("$[0].discordId").value(nullValue()))
                .andExpect(jsonPath("$[0].visibility.discord").value(false));
    }

    @Test
    void leavesOutContactsWhoHideTheirName() throws Exception {
        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());
        anna.setShowName(false);
        userRepository.save(anna);

        mockMvc.perform(get(CONTACTS).with(as(owner)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void profileTellsWhetherTheAlumnIsAContact() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", anna.getId()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isContact").value(false));

        mockMvc.perform(put(CONTACTS + "/{id}", anna.getId()).with(as(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alumni/{id}", anna.getId()).with(as(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isContact").value(true));
    }

    @Test
    void rejectsUserWhoIsNotAVerifiedAlumn() throws Exception {
        mockMvc.perform(get(CONTACTS).with(JwtTestSupport.asUser())).andExpect(status().isForbidden());
    }

    private RequestPostProcessor as(User user) {
        return JwtTestSupport.asVerifiedAlumn(user.getKeycloakId());
    }

    private User approvedAlumn(String firstName, String lastName) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user = userRepository.save(user);
        applicationRepository.save(Application.builder()
                .applicant(user)
                .status(ApplicationStatus.APPROVED)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .reviewedAt(Instant.parse("2021-06-15T10:00:00Z"))
                .build());
        return user;
    }
}
