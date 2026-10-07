package pl.edu.agh.backend.survey;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
    private static final String OTHER_ALUMN_SUBJECT = "88888888-8888-8888-8888-888888888888";

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

    @Test
    void enforcesRoles() throws Exception {
        mockMvc.perform(get("/api/alumni/surveys")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/alumni/surveys").with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/surveys").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void draftSurveyIsHiddenFromAlumniAndRejectsSubmissions() throws Exception {
        String created = createSurvey("DRAFT", """
                {"content":"Uwagi","type":"TEXT","options":[]}
                """);
        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID questionId = UUID.fromString(JsonPath.read(created, "$.questions[0].id"));

        mockMvc.perform(get("/api/alumni/surveys").with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/alumni/surveys/{id}", surveyId).with(JwtTestSupport.asVerifiedAlumn(ALUMN_SUBJECT)))
                .andExpect(status().isConflict());

        mockMvc.perform(submit(surveyId, """
                        {"answers":[{"questionId":"%s","value":"Coś"}]}
                        """.formatted(questionId))).andExpect(status().isConflict());
    }

    @Test
    void rejectsIncompleteOrForeignAnswers() throws Exception {
        String created = createSurvey("ACTIVE", """
                {"content":"Język?","type":"SINGLE_CHOICE","options":["Java","Kotlin"]},
                {"content":"Framework?","type":"SINGLE_CHOICE","options":["Spring","Quarkus"]}
                """);
        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID firstQuestionId = UUID.fromString(JsonPath.read(created, "$.questions[0].id"));
        UUID secondQuestionId = UUID.fromString(JsonPath.read(created, "$.questions[1].id"));
        UUID firstQuestionOption = UUID.fromString(JsonPath.read(created, "$.questions[0].options[0].id"));
        UUID secondQuestionOption = UUID.fromString(JsonPath.read(created, "$.questions[1].options[0].id"));

        mockMvc.perform(submit(surveyId, """
                        {"answers":[{"questionId":"%s","value":"%s"}]}
                        """.formatted(firstQuestionId, firstQuestionOption)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(submit(surveyId, """
                        {"answers":[{"questionId":"%s","value":"%s"},{"questionId":"%s","value":"%s"}]}
                        """.formatted(
                                firstQuestionId, secondQuestionOption, secondQuestionId, secondQuestionOption)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(submit(surveyId, """
                        {"answers":[{"questionId":"%s","value":"not-a-uuid"},{"questionId":"%s","value":"%s"}]}
                        """.formatted(firstQuestionId, secondQuestionId, secondQuestionOption)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aggregatesMultiChoiceAnswers() throws Exception {
        String created = createSurvey("ACTIVE", """
                {"content":"Technologie?","type":"MULTI_CHOICE","options":["Java","Kotlin","Go"]}
                """);
        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID questionId = UUID.fromString(JsonPath.read(created, "$.questions[0].id"));
        String java = JsonPath.read(created, "$.questions[0].options[0].id");
        String kotlin = JsonPath.read(created, "$.questions[0].options[1].id");

        mockMvc.perform(submit(surveyId, ALUMN_SUBJECT, """
                        {"answers":[{"questionId":"%s","value":"%s,%s"}]}
                        """.formatted(questionId, java, kotlin)))
                .andExpect(status().isCreated());
        mockMvc.perform(submit(surveyId, OTHER_ALUMN_SUBJECT, """
                        {"answers":[{"questionId":"%s","value":"%s"}]}
                        """.formatted(questionId, java)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/surveys/{id}/results", surveyId).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionCount").value(2))
                .andExpect(jsonPath("$.questions[0].optionCounts[0].count").value(2))
                .andExpect(jsonPath("$.questions[0].optionCounts[1].count").value(1))
                .andExpect(jsonPath("$.questions[0].optionCounts[2].count").value(0));
    }

    @Test
    void surveyWithSubmissionsCannotBeEditedOrDeleted() throws Exception {
        String created = createSurvey("ACTIVE", """
                {"content":"Uwagi","type":"TEXT","options":[]}
                """);
        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID questionId = UUID.fromString(JsonPath.read(created, "$.questions[0].id"));

        mockMvc.perform(submit(surveyId, """
                        {"answers":[{"questionId":"%s","value":"Super"}]}
                        """.formatted(questionId))).andExpect(status().isCreated());

        mockMvc.perform(put("/api/admin/surveys/{id}", surveyId)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(surveyBody("CLOSED", """
                                {"content":"Uwagi","type":"TEXT","options":[]}
                                """)))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/admin/surveys/{id}", surveyId)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void adminUpdatesAndDeletesSurveyWithoutSubmissions() throws Exception {
        String created = createSurvey("DRAFT", """
                {"content":"Uwagi","type":"TEXT","options":[]}
                """);
        UUID surveyId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(put("/api/admin/surveys/{id}", surveyId)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(surveyBody("ACTIVE", """
                                {"content":"Język?","type":"SINGLE_CHOICE","options":["Java"]}
                                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].type").value("SINGLE_CHOICE"));

        mockMvc.perform(delete("/api/admin/surveys/{id}", surveyId)
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/surveys/{id}", surveyId).with(JwtTestSupport.asAdmin(ADMIN_SUBJECT)))
                .andExpect(status().isNotFound());
    }

    private String createSurvey(String status, String questionsJson) throws Exception {
        return mockMvc.perform(post("/api/admin/surveys")
                        .with(JwtTestSupport.asAdmin(ADMIN_SUBJECT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(surveyBody(status, questionsJson)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static String surveyBody(String status, String questionsJson) {
        Instant endsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        return """
                {"title":"Ankieta","endsAt":"%s","status":"%s","questions":[%s]}
                """.formatted(endsAt, status, questionsJson);
    }

    private static MockHttpServletRequestBuilder submit(UUID surveyId, String body) {
        return submit(surveyId, ALUMN_SUBJECT, body);
    }

    private static MockHttpServletRequestBuilder submit(UUID surveyId, String subject, String body) {
        return post("/api/alumni/surveys/{id}/submissions", surveyId)
                .with(JwtTestSupport.asVerifiedAlumn(subject))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
