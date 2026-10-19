package pl.edu.agh.backend.survey;

/** An admin's survey definition breaks a rule bean validation can't express, e.g. an ACTIVE survey ending in the past. */
public class InvalidSurveyException extends RuntimeException {

    public InvalidSurveyException(String message) {
        super(message);
    }
}
