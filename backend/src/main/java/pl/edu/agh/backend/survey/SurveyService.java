package pl.edu.agh.backend.survey;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final CallerUserService callerUserService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<SurveyResponse> listActive(Pageable pageable) {
        return surveyRepository
                .findAllByStatusAndEndsAtAfterOrderByEndsAtAsc(SurveyStatus.ACTIVE, Instant.now(clock), pageable)
                .map(SurveyResponse::from);
    }

    @Transactional(readOnly = true)
    public SurveyResponse getActive(UUID id) {
        Survey survey = surveyRepository.findById(id).orElseThrow(SurveyNotFoundException::new);
        ensureActive(survey);
        return SurveyResponse.from(survey);
    }

    /**
     * Checking for an existing submission and inserting is a check-then-act, so it opens by
     * locking the survey row (mirrors {@code EventRegistrationService.register}); this also
     * serializes submission against an admin edit/delete that requires "no submissions yet" (see
     * {@code AdminSurveyService}), which lock the same row.
     */
    @Transactional
    public SubmitSurveyResponse submit(UUID surveyId, Caller caller, SubmitSurveyRequest request) {
        Survey survey = surveyRepository.findForUpdateById(surveyId).orElseThrow(SurveyNotFoundException::new);
        ensureActive(survey);
        User user = callerUserService.getOrCreate(caller);
        if (surveySubmissionRepository.existsBySurveyIdAndUserId(surveyId, user.getId())) {
            throw new SurveyAlreadySubmittedException();
        }

        Map<UUID, SurveyQuestion> questionsById =
                survey.getQuestions().stream().collect(Collectors.toMap(SurveyQuestion::getId, Function.identity()));
        if (request.answers().size() != questionsById.size()) {
            throw new InvalidSurveyAnswerException("All survey questions must be answered exactly once");
        }

        SurveySubmission submission = new SurveySubmission();
        submission.setSurvey(survey);
        submission.setUser(user);
        submission.setSubmittedAt(Instant.now(clock));

        Set<UUID> answeredQuestionIds = new HashSet<>();
        for (SubmitSurveyRequest.AnswerInput answerInput : request.answers()) {
            SurveyQuestion question = questionsById.get(answerInput.questionId());
            if (question == null) {
                throw new InvalidSurveyAnswerException("Unknown question id: " + answerInput.questionId());
            }
            if (!answeredQuestionIds.add(question.getId())) {
                throw new InvalidSurveyAnswerException("Duplicate answer for question: " + question.getId());
            }
            String normalizedValue = validateAndNormalizeAnswer(question, answerInput.value());
            SurveyAnswer answer = new SurveyAnswer();
            answer.setSubmission(submission);
            answer.setSurvey(survey);
            answer.setQuestion(question);
            answer.setValue(normalizedValue);
            submission.getAnswers().add(answer);
        }

        try {
            SurveySubmission saved = surveySubmissionRepository.saveAndFlush(submission);
            return new SubmitSurveyResponse(saved.getId(), surveyId, saved.getSubmittedAt());
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateSurveySubmission(ex)) {
                throw new SurveyAlreadySubmittedException();
            }
            throw ex;
        }
    }

    /**
     * A draft was never published, so it is reported as missing — like a hidden event — and its id can't be
     * probed for. A closed or expired survey was public once, so it gets the more useful "not active".
     */
    private void ensureActive(Survey survey) {
        if (survey.getStatus() == SurveyStatus.DRAFT) {
            throw new SurveyNotFoundException();
        }
        if (survey.getStatus() != SurveyStatus.ACTIVE || !survey.getEndsAt().isAfter(Instant.now(clock))) {
            throw new SurveyNotActiveException();
        }
    }

    private String validateAndNormalizeAnswer(SurveyQuestion question, String rawValue) {
        return switch (question.getType()) {
            case TEXT -> {
                if (rawValue.isBlank()) {
                    throw new InvalidSurveyAnswerException("Text answer cannot be blank");
                }
                yield rawValue.trim();
            }
            case SINGLE_CHOICE -> {
                UUID optionId = parseOptionId(rawValue);
                ensureOptionBelongsToQuestion(question, optionId);
                yield optionId.toString();
            }
            case MULTI_CHOICE -> {
                Set<UUID> optionIds = SurveyAnswerValues.commaSeparated(rawValue).stream()
                        .map(this::parseOptionId)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (optionIds.isEmpty()) {
                    throw new InvalidSurveyAnswerException("Multi-choice answer requires at least one option");
                }
                optionIds.forEach(optionId -> ensureOptionBelongsToQuestion(question, optionId));
                yield optionIds.stream().map(UUID::toString).collect(Collectors.joining(","));
            }
        };
    }

    private UUID parseOptionId(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new InvalidSurveyAnswerException("Invalid option id: " + value);
        }
    }

    private void ensureOptionBelongsToQuestion(SurveyQuestion question, UUID optionId) {
        boolean found =
                question.getOptions().stream().anyMatch(option -> option.getId().equals(optionId));
        if (!found) {
            throw new InvalidSurveyAnswerException("Option does not belong to question: " + question.getId());
        }
    }

    private boolean isDuplicateSurveySubmission(DataIntegrityViolationException ex) {
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return "uq_survey_submissions_user_survey".equals(violation.getConstraintName());
            }
        }
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains("uq_survey_submissions_user_survey");
    }
}
