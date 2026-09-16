package pl.edu.agh.backend.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@Testcontainers
@Transactional
class SurveyEntityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveySubmissionRepository surveySubmissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void persistsSurveyWithQuestionsAndOptions() {
        Survey survey = new Survey();
        survey.setTitle("Ankieta");
        survey.setEndsAt(Instant.now().plus(7, ChronoUnit.DAYS));
        survey.setStatus(SurveyStatus.DRAFT);

        SurveyQuestion question = new SurveyQuestion();
        question.setSurvey(survey);
        question.setContent("Ulubiony język?");
        question.setType(QuestionType.SINGLE_CHOICE);
        question.setDisplayOrder(0);

        SurveyQuestionOption option = new SurveyQuestionOption();
        option.setQuestion(question);
        option.setLabel("Java");
        option.setDisplayOrder(0);
        question.getOptions().add(option);
        survey.getQuestions().add(question);

        Survey saved = surveyRepository.saveAndFlush(survey);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQuestions()).hasSize(1);
        assertThat(saved.getQuestions().getFirst().getOptions()).hasSize(1);
    }

    @Test
    void rejectsAnswerWhoseQuestionBelongsToADifferentSurvey() {
        Survey surveyA = persistSurveyWithOneQuestion("Ankieta A");
        Survey surveyB = persistSurveyWithOneQuestion("Ankieta B");
        User user = persistUser();

        SurveySubmission submission = new SurveySubmission();
        submission.setSurvey(surveyA);
        submission.setUser(user);
        submission.setSubmittedAt(Instant.now());

        SurveyAnswer answer = new SurveyAnswer();
        answer.setSubmission(submission);
        // Mismatch: submission belongs to surveyA, but the answer's survey_id still points at
        // surveyA while the question belongs to surveyB. The composite question FK rejects this.
        answer.setSurvey(surveyA);
        answer.setQuestion(surveyB.getQuestions().getFirst());
        answer.setValue("Java");
        submission.getAnswers().add(answer);

        assertThatThrownBy(() -> surveySubmissionRepository.saveAndFlush(submission))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnswerWhoseSurveyDoesNotMatchItsSubmission() {
        Survey surveyA = persistSurveyWithOneQuestion("Ankieta A");
        Survey surveyB = persistSurveyWithOneQuestion("Ankieta B");
        User user = persistUser();

        SurveySubmission submission = new SurveySubmission();
        submission.setSurvey(surveyA);
        submission.setUser(user);
        submission.setSubmittedAt(Instant.now());

        SurveyAnswer answer = new SurveyAnswer();
        answer.setSubmission(submission);
        // Mismatch: submission belongs to surveyA, but answer.survey_id points at surveyB while
        // the question is from surveyB. The composite submission FK rejects this even though the
        // question FK alone would pass.
        answer.setSurvey(surveyB);
        answer.setQuestion(surveyB.getQuestions().getFirst());
        answer.setValue("Java");
        submission.getAnswers().add(answer);

        assertThatThrownBy(() -> surveySubmissionRepository.saveAndFlush(submission))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User persistUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    private Survey persistSurveyWithOneQuestion(String title) {
        Survey survey = new Survey();
        survey.setTitle(title);
        survey.setEndsAt(Instant.now().plus(7, ChronoUnit.DAYS));
        survey.setStatus(SurveyStatus.DRAFT);

        SurveyQuestion question = new SurveyQuestion();
        question.setSurvey(survey);
        question.setContent("Pytanie");
        question.setType(QuestionType.TEXT);
        question.setDisplayOrder(0);
        survey.getQuestions().add(question);

        return surveyRepository.saveAndFlush(survey);
    }
}
