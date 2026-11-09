package pl.edu.agh.backend.survey;

public class SurveyNotActiveException extends RuntimeException {

    public SurveyNotActiveException() {
        super("Survey is not active");
    }
}
