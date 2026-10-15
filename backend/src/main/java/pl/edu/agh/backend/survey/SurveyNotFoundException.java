package pl.edu.agh.backend.survey;

public class SurveyNotFoundException extends RuntimeException {

    public SurveyNotFoundException() {
        super("Survey not found");
    }
}
