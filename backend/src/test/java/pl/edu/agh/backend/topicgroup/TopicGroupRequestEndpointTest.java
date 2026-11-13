package pl.edu.agh.backend.topicgroup;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class TopicGroupRequestEndpointTest {

    private static final String MY_REQUESTS = "/api/alumni/me/topic-group-requests";
    private static final String ADMIN_REQUESTS = "/api/admin/topic-group-requests";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User anna;
    private User admin;

    @BeforeEach
    void setUp() {
        anna = user("Anna", "Nowak");
        admin = user("Adam", "Admin");
    }

    @Test
    void alumnSubmitsARequestAndSeesItAwaitingReview() throws Exception {
        submit(anna, "Grupa AI", "Wymiana doświadczeń z wdrożeń AI.")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grupa AI"))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(get(MY_REQUESTS).with(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Grupa AI"))
                .andExpect(jsonPath("$[0].description").value("Wymiana doświadczeń z wdrożeń AI."))
                .andExpect(jsonPath("$[0].status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$[0].requester.firstName").value("Anna"))
                .andExpect(jsonPath("$[0].requester.lastName").value("Nowak"));
    }

    @Test
    void alumnCannotHaveTwoRequestsAwaitingReview() throws Exception {
        submit(anna, "Grupa AI", "Opis").andExpect(status().isCreated());

        submit(anna, "Grupa chmurowa", "Opis").andExpect(status().isConflict());
    }

    @Test
    void requestNeedsANameAndADescription() throws Exception {
        submit(anna, " ", "Opis").andExpect(status().isBadRequest());
        submit(anna, "Grupa AI", "").andExpect(status().isBadRequest());
    }

    @Test
    void adminSeesRequestsAwaitingReview() throws Exception {
        submit(anna, "Grupa AI", "Wymiana doświadczeń z wdrożeń AI.").andExpect(status().isCreated());

        mockMvc.perform(get(ADMIN_REQUESTS).with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Grupa AI"))
                .andExpect(jsonPath("$.content[0].status").value("UNDER_REVIEW"))
                .andExpect(
                        jsonPath("$.content[0].requester.id").value(anna.getId().toString()))
                .andExpect(jsonPath("$.content[0].requester.firstName").value("Anna"));
    }

    @Test
    void adminApprovesARequest() throws Exception {
        String id = submittedId(anna, "Grupa AI");

        mockMvc.perform(post(ADMIN_REQUESTS + "/{id}/approve", id)
                        .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        mockMvc.perform(get(MY_REQUESTS).with(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId())))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
        mockMvc.perform(get(ADMIN_REQUESTS).with(JwtTestSupport.asAdmin(admin.getKeycloakId())))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void approvingCreatesTheGroupSoItsNameIsTaken() throws Exception {
        approve(submittedId(anna, "Grupa AI")).andExpect(status().isOk());

        submit(user("Piotr", "Zieliński"), " grupa ai ", "Opis").andExpect(status().isConflict());
    }

    @Test
    void approvingFailsWhenTheNameWasTakenInTheMeantime() throws Exception {
        String first = submittedId(anna, "Grupa AI");
        String second = submittedId(user("Piotr", "Zieliński"), "GRUPA AI");
        approve(first).andExpect(status().isOk());

        approve(second).andExpect(status().isConflict());
    }

    @Test
    void aReviewedRequestCannotBeApprovedAgain() throws Exception {
        String id = submittedId(anna, "Grupa AI");
        approve(id).andExpect(status().isOk());

        approve(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid topic group request state"));
    }

    @Test
    void approvingAnUnknownRequestReturns404() throws Exception {
        approve(UUID.randomUUID().toString()).andExpect(status().isNotFound());
    }

    @Test
    void adminRejectsARequestAndTheAlumnSeesWhy() throws Exception {
        String id = submittedId(anna, "Grupa AI");

        reject(id, """
                        {"reason": "Taka grupa już działa na Discordzie."}
                        """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get(MY_REQUESTS).with(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId())))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectionReason").value("Taka grupa już działa na Discordzie."))
                .andExpect(jsonPath("$[0].reviewedAt").exists());
    }

    @Test
    void rejectingNeedsAReason() throws Exception {
        String id = submittedId(anna, "Grupa AI");

        reject(id, "{\"reason\": \" \"}").andExpect(status().isBadRequest());
        reject(id, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void alumnCanAskAgainAfterARejection() throws Exception {
        reject(submittedId(anna, "Grupa AI"), "{\"reason\": \"Zbyt wąski temat.\"}")
                .andExpect(status().isOk());

        submit(anna, "Grupa AI i ML", "Szerszy temat").andExpect(status().isCreated());
    }

    @Test
    void onlyAdminsReviewAndOnlyVerifiedAlumniAsk() throws Exception {
        mockMvc.perform(get(ADMIN_REQUESTS).with(JwtTestSupport.asVerifiedAlumn(anna.getKeycloakId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(MY_REQUESTS)
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Grupa AI\", \"description\": \"Opis\"}"))
                .andExpect(status().isForbidden());
    }

    private ResultActions reject(String requestId, String body) throws Exception {
        return mockMvc.perform(post(ADMIN_REQUESTS + "/{id}/reject", requestId)
                .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions approve(String requestId) throws Exception {
        return mockMvc.perform(post(ADMIN_REQUESTS + "/{id}/approve", requestId)
                .with(JwtTestSupport.asAdmin(admin.getKeycloakId()))
                .with(csrf()));
    }

    private String submittedId(User requester, String name) throws Exception {
        String body = submit(requester, name, "Opis")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions submit(User requester, String name, String description) throws Exception {
        return mockMvc.perform(post(MY_REQUESTS)
                .with(JwtTestSupport.asVerifiedAlumn(requester.getKeycloakId()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "description": "%s"}
                        """.formatted(name, description)));
    }

    private User user(String firstName, String lastName) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return userRepository.save(user);
    }
}
