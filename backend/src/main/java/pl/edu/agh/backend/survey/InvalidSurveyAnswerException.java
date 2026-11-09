package pl.edu.agh.backend.survey;

public class InvalidSurveyAnswerException extends RuntimeException {

    public InvalidSurveyAnswerException(String message) {
        super(message);
    }
}
