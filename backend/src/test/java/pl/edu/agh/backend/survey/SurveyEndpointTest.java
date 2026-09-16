package pl.edu.agh.backend.survey;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class SurveyEndpointTest {

    private static final String ADMIN_SUBJECT = "66666666-6666-6666-6666-666666666666";
    private static final String ALUMN_SUBJECT = "77777777-7777-7777-7777-777777777777";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveySubmissionRepository surveySubmissionRepository;

    @BeforeEach
    void cleanSurveys() {
        surveySubmissionRepository.deleteAll();
        surveyRepository.deleteAll();
    }

    @Test
    void adminCreatesSurveyAlumniSubmitsAndAdminReadsResults() throws Exception {
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        String created = mockMvc.perform(post("/api/admin/surveys")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Ankieta satysfakcji",
                                  "description":"Krótka ankieta",
                                  "endsAt":"%s",
                                  "status":"ACTIVE",
                                  "questions":[
                                    {"content":"Ulubiony język?","type":"SINGLE_CHOICE","options":["Java","Kotlin"]},
                                    {"content":"Uwagi","type":"TEXT","options":[]}
                                  ]
                                }
                                """.formatted(endsAt)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID singleChoiceQuestionId = UUID.fromString(JsonPath.read(created, "$.questions[0].id"));
        UUID optionId = UUID.fromString(JsonPath.read(created, "$.questions[0].options[0].id"));
        UUID textQuestionId = UUID.fromString(JsonPath.read(created, "$.questions[1].id"));

        mockMvc.perform(get("/api/alumni/surveys").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(surveyId.toString()));

        mockMvc.perform(post("/api/alumni/surveys/{id}/submissions", surveyId)
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answers":[
                                    {"questionId":"%s","value":"%s"},
                                    {"questionId":"%s","value":"Super wydarzenie"}
                                  ]
                                }
                                """.formatted(singleChoiceQuestionId, optionId, textQuestionId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/alumni/surveys/{id}/submissions", surveyId)
                        .with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers":[{"questionId":"%s","value":"%s"},{"questionId":"%s","value":"x"}]}
                                """.formatted(singleChoiceQuestionId, optionId, textQuestionId)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/admin/surveys/{id}/results", surveyId).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionCount").value(1))
                .andExpect(jsonPath("$.questions[0].optionCounts[0].count").value(1))
                .andExpect(jsonPath("$.questions[1].textAnswers[0]").value("Super wydarzenie"));
    }

    @Test
    void rejectsSurveyWithOversizedChoiceOptionLabel() throws Exception {
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        String oversizedLabel = "x".repeat(501);

        mockMvc.perform(post("/api/admin/surveys")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Ankieta",
                                  "endsAt":"%s",
                                  "status":"DRAFT",
                                  "questions":[
                                    {"content":"Pytanie","type":"SINGLE_CHOICE","options":["%s"]}
                                  ]
                                }
                                """.formatted(endsAt, oversizedLabel)))
                .andExpect(status().isBadRequest());
    }
}
