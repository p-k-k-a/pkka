package pl.edu.agh.backend.survey;

public class SurveyAlreadySubmittedException extends RuntimeException {

    public SurveyAlreadySubmittedException() {
        super("Survey has already been submitted by this user");
    }
}
