package pl.edu.agh.backend.survey;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;

    @Transactional(readOnly = true)
    public Page<AdminSurveyListItemResponse> list(Pageable pageable) {
        return surveyRepository.findAllByOrderByCreatedAtDesc(pageable).map(AdminSurveyListItemResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminSurveyResponse get(UUID id) {
        return surveyRepository.findById(id).map(AdminSurveyResponse::from).orElseThrow(SurveyNotFoundException::new);
    }

    @Transactional
    public AdminSurveyResponse create(CreateSurveyRequest request) {
        Survey survey = new Survey();
        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setEndsAt(request.endsAt());
        SurveyStatus status = request.status() != null ? request.status() : SurveyStatus.DRAFT;
        validateStatusAndEndDate(status, request.endsAt());
        survey.setStatus(status);
        applyQuestions(survey, request.questions());
        return AdminSurveyResponse.from(surveyRepository.saveAndFlush(survey));
    }

    /**
     * Locks the survey row before checking for submissions (mirrors {@code SurveyService.submit},
     * which takes the same lock): otherwise a submission could be inserted between the
     * "no submissions yet" check and this update/delete going through.
     */
    @Transactional
    public AdminSurveyResponse update(UUID id, UpdateSurveyRequest request) {
        Survey survey = surveyRepository.findForUpdateById(id).orElseThrow(SurveyNotFoundException::new);
        ensureNoSubmissions(id);
        validateStatusAndEndDate(request.status(), request.endsAt());
        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setEndsAt(request.endsAt());
        survey.setStatus(request.status());
        applyQuestions(survey, request.questions());
        return AdminSurveyResponse.from(surveyRepository.saveAndFlush(survey));
    }

    @Transactional
    public void delete(UUID id) {
        Survey survey = surveyRepository.findForUpdateById(id).orElseThrow(SurveyNotFoundException::new);
        ensureNoSubmissions(id);
        surveyRepository.delete(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResultsResponse getResults(UUID id) {
        Survey survey = surveyRepository.findById(id).orElseThrow(SurveyNotFoundException::new);
        List<SurveySubmission> submissions = surveySubmissionRepository.findAllBySurveyId(id);

        // Group every answer by question id once (O(submissions x answers)) instead of having
        // aggregateQuestion re-scan every submission's answers for every question
        // (O(questions x submissions x answers)). With Q questions and roughly Q answers per
        // submission, that previously degraded to O(submissions x questions^2).
        Map<UUID, List<SurveyAnswer>> answersByQuestionId = submissions.stream()
                .flatMap(submission -> submission.getAnswers().stream())
                .collect(Collectors.groupingBy(answer -> answer.getQuestion().getId()));

        List<SurveyResultsResponse.QuestionResult> questionResults = survey.getQuestions().stream()
                .map(question ->
                        aggregateQuestion(question, answersByQuestionId.getOrDefault(question.getId(), List.of())))
                .toList();
        return new SurveyResultsResponse(survey.getId(), submissions.size(), questionResults);
    }

    private SurveyResultsResponse.QuestionResult aggregateQuestion(
            SurveyQuestion question, List<SurveyAnswer> answers) {
        if (question.getType() == QuestionType.TEXT) {
            List<String> textAnswers =
                    answers.stream().map(SurveyAnswer::getValue).toList();
            return new SurveyResultsResponse.QuestionResult(
                    question.getId(), question.getContent(), question.getType(), List.of(), textAnswers);
        }

        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (SurveyQuestionOption option : question.getOptions()) {
            counts.put(option.getId(), 0L);
        }

        answers.forEach(answer -> {
            for (String part : SurveyAnswerValues.commaSeparated(answer.getValue())) {
                UUID optionId = UUID.fromString(part);
                counts.computeIfPresent(optionId, (key, value) -> value + 1);
            }
        });

        List<SurveyResultsResponse.OptionCount> optionCounts = question.getOptions().stream()
                .map(option -> new SurveyResultsResponse.OptionCount(
                        option.getId(), option.getLabel(), counts.getOrDefault(option.getId(), 0L)))
                .toList();

        return new SurveyResultsResponse.QuestionResult(
                question.getId(), question.getContent(), question.getType(), optionCounts, List.of());
    }

    private void applyQuestions(Survey survey, List<SurveyQuestionRequest> questions) {
        survey.getQuestions().clear();
        for (int i = 0; i < questions.size(); i++) {
            SurveyQuestionRequest questionRequest = questions.get(i);
            validateQuestionRequest(questionRequest);
            SurveyQuestion question = new SurveyQuestion();
            question.setSurvey(survey);
            question.setContent(questionRequest.content());
            question.setType(questionRequest.type());
            question.setDisplayOrder(i);
            if (questionRequest.type() != QuestionType.TEXT) {
                List<String> options = questionRequest.options();
                for (int j = 0; j < options.size(); j++) {
                    SurveyQuestionOption option = new SurveyQuestionOption();
                    option.setQuestion(question);
                    option.setLabel(options.get(j));
                    option.setDisplayOrder(j);
                    question.getOptions().add(option);
                }
            }
            survey.getQuestions().add(question);
        }
    }

    private void ensureNoSubmissions(UUID surveyId) {
        // existsBySurveyId, not findAllBySurveyId(...).isEmpty(): the latter's @EntityGraph
        // fetch-joins answers and questions just to check a boolean.
        if (surveySubmissionRepository.existsBySurveyId(surveyId)) {
            throw new SurveyHasSubmissionsException();
        }
    }

    private void validateStatusAndEndDate(SurveyStatus status, Instant endsAt) {
        if (status == SurveyStatus.ACTIVE && !endsAt.isAfter(Instant.now())) {
            throw new InvalidSurveyAnswerException("Active surveys must end in the future");
        }
    }

    private void validateQuestionRequest(SurveyQuestionRequest questionRequest) {
        if (questionRequest.type() == QuestionType.TEXT) {
            return;
        }
        if (questionRequest.options() == null || questionRequest.options().isEmpty()) {
            throw new InvalidSurveyAnswerException("Choice questions require at least one option");
        }
    }
}
