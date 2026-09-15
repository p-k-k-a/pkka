package pl.edu.agh.backend.infrastructure;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.edu.agh.backend.alumni.AlumniNotFoundException;
import pl.edu.agh.backend.application.ApplicationAlreadyExistsException;
import pl.edu.agh.backend.application.ApplicationNotFoundException;
import pl.edu.agh.backend.application.InvalidApplicationStateException;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.registration.EventRegistrationConflictException;
import pl.edu.agh.backend.event.registration.EventRegistrationNotFoundException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleAssignmentException;
import pl.edu.agh.backend.post.PostAlreadyPublishedException;
import pl.edu.agh.backend.post.PostNotFoundException;
import pl.edu.agh.backend.survey.InvalidSurveyAnswerException;
import pl.edu.agh.backend.survey.SurveyAlreadySubmittedException;
import pl.edu.agh.backend.survey.SurveyHasSubmissionsException;
import pl.edu.agh.backend.survey.SurveyNotActiveException;
import pl.edu.agh.backend.survey.SurveyNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AlumniNotFoundException.class)
    public ProblemDetail handleAlumniNotFound(AlumniNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Alumni not found");
        return problem;
    }

    @ExceptionHandler(PostNotFoundException.class)
    public ProblemDetail handlePostNotFound(PostNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Post not found");
        return problem;
    }

    @ExceptionHandler(PostAlreadyPublishedException.class)
    public ProblemDetail handlePostAlreadyPublished(PostAlreadyPublishedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Post already published");
        return problem;
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleEventNotFound(EventNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Event not found");
        return problem;
    }

    @ExceptionHandler(EventRegistrationNotFoundException.class)
    public ProblemDetail handleEventRegistrationNotFound(EventRegistrationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Event registration not found");
        return problem;
    }

    @ExceptionHandler(EventRegistrationConflictException.class)
    public ProblemDetail handleEventRegistrationConflict(EventRegistrationConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Event registration conflict");
        problem.setProperty("reason", ex.getReason().name());
        return problem;
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ProblemDetail handleApplicationNotFound(ApplicationNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Application not found");
        return problem;
    }

    @ExceptionHandler(ApplicationAlreadyExistsException.class)
    public ProblemDetail handleApplicationAlreadyExists(ApplicationAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Application already exists");
        return problem;
    }

    @ExceptionHandler(InvalidApplicationStateException.class)
    public ProblemDetail handleInvalidApplicationState(InvalidApplicationStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid application state");
        return problem;
    }

    @ExceptionHandler(KeycloakRoleAssignmentException.class)
    public ProblemDetail handleKeycloakRoleAssignment(KeycloakRoleAssignmentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Failed to update user role in identity provider");
        problem.setTitle("Identity provider error");
        return problem;
    }

    @ExceptionHandler(SurveyNotFoundException.class)
    public ProblemDetail handleSurveyNotFound(SurveyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Survey not found");
        return problem;
    }

    @ExceptionHandler(SurveyNotActiveException.class)
    public ProblemDetail handleSurveyNotActive(SurveyNotActiveException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Survey not active");
        return problem;
    }

    @ExceptionHandler(SurveyAlreadySubmittedException.class)
    public ProblemDetail handleSurveyAlreadySubmitted(SurveyAlreadySubmittedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Survey already submitted");
        return problem;
    }

    @ExceptionHandler(InvalidSurveyAnswerException.class)
    public ProblemDetail handleInvalidSurveyAnswer(InvalidSurveyAnswerException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid survey answer");
        return problem;
    }

    @ExceptionHandler(SurveyHasSubmissionsException.class)
    public ProblemDetail handleSurveyHasSubmissions(SurveyHasSubmissionsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Survey has submissions");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Validation failed");
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }
}
