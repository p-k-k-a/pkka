package pl.edu.agh.backend.survey;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
class SurveyEntityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private SurveyRepository surveyRepository;

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
}
