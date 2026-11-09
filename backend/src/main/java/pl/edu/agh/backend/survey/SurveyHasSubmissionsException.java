package pl.edu.agh.backend.survey;

public class SurveyHasSubmissionsException extends RuntimeException {

    public SurveyHasSubmissionsException() {
        super("Survey cannot be modified after submissions exist");
    }
}
