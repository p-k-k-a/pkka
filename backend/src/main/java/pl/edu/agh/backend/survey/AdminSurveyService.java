package pl.edu.agh.backend.survey;

import java.time.Instant;
import java.util.*;
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

    @Transactional
    public AdminSurveyResponse update(UUID id, UpdateSurveyRequest request) {
        Survey survey = surveyRepository.findById(id).orElseThrow(SurveyNotFoundException::new);
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
        Survey survey = surveyRepository.findById(id).orElseThrow(SurveyNotFoundException::new);
        ensureNoSubmissions(id);
        surveyRepository.delete(survey);
    }

    @Transactional(readOnly = true)
    public SurveyResultsResponse getResults(UUID id) {
        Survey survey = surveyRepository.findById(id).orElseThrow(SurveyNotFoundException::new);
        List<SurveySubmission> submissions = surveySubmissionRepository.findAllBySurveyId(id);
        List<SurveyResultsResponse.QuestionResult> questionResults = survey.getQuestions().stream()
                .map(question -> aggregateQuestion(question, submissions))
                .toList();
        return new SurveyResultsResponse(survey.getId(), submissions.size(), questionResults);
    }

    private SurveyResultsResponse.QuestionResult aggregateQuestion(
            SurveyQuestion question, List<SurveySubmission> submissions) {
        if (question.getType() == QuestionType.TEXT) {
            List<String> textAnswers = submissions.stream()
                    .flatMap(submission -> submission.getAnswers().stream())
                    .filter(answer -> answer.getQuestion().getId().equals(question.getId()))
                    .map(SurveyAnswer::getValue)
                    .toList();
            return new SurveyResultsResponse.QuestionResult(
                    question.getId(), question.getContent(), question.getType(), List.of(), textAnswers);
        }

        Map<UUID, Long> counts = new LinkedHashMap<>();
        for (SurveyQuestionOption option : question.getOptions()) {
            counts.put(option.getId(), 0L);
        }

        submissions.stream()
                .flatMap(submission -> submission.getAnswers().stream())
                .filter(answer -> answer.getQuestion().getId().equals(question.getId()))
                .forEach(answer -> {
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
        if (!surveySubmissionRepository.findAllBySurveyId(surveyId).isEmpty()) {
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
