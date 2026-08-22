package pl.edu.agh.backend.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import pl.edu.agh.backend.alumni.AlumniNotFoundException;
import pl.edu.agh.backend.application.ApplicationAlreadyExistsException;
import pl.edu.agh.backend.application.ApplicationNotFoundException;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.application.InvalidApplicationStateException;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakRoleAssignmentException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleAlumniNotFoundReturns404() {
        ProblemDetail detail = handler.handleAlumniNotFound(new AlumniNotFoundException(UUID.randomUUID()));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Alumni not found");
    }

    @Test
    void handleEventNotFoundReturns404() {
        ProblemDetail detail = handler.handleEventNotFound(new EventNotFoundException(UUID.randomUUID()));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Event not found");
    }

    @Test
    void handleApplicationNotFoundReturns404() {
        ProblemDetail detail = handler.handleApplicationNotFound(new ApplicationNotFoundException());
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(detail.getTitle()).isEqualTo("Application not found");
    }

    @Test
    void handleApplicationAlreadyExistsReturns409() {
        ProblemDetail detail = handler.handleApplicationAlreadyExists(new ApplicationAlreadyExistsException());
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getTitle()).isEqualTo("Application already exists");
    }

    @Test
    void handleInvalidApplicationStateReturns409() {
        ProblemDetail detail = handler.handleInvalidApplicationState(
                new InvalidApplicationStateException(UUID.randomUUID(), ApplicationStatus.APPROVED));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getTitle()).isEqualTo("Invalid application state");
    }

    @Test
    void handleKeycloakRoleAssignmentReturns502() {
        ProblemDetail detail =
                handler.handleKeycloakRoleAssignment(new KeycloakRoleAssignmentException("failed", null));
        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
        assertThat(detail.getTitle()).isEqualTo("Identity provider error");
    }

    @Test
    void handleValidationReturns400WithFieldErrors() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "request");
        binding.addError(new FieldError("request", "fieldOfStudy", "must not be blank"));
        var ex = new MethodArgumentNotValidException(null, binding);

        ProblemDetail detail = handler.handleValidation(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Validation failed");
        @SuppressWarnings("unchecked")
        var errors = (java.util.Map<String, String>) detail.getProperties().get("errors");
        assertThat(errors).containsEntry("fieldOfStudy", "must not be blank");
    }
}
