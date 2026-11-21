package pl.edu.agh.backend.topicgroup;

public class TopicGroupCoordinatorCannotLeaveException extends RuntimeException {

    public TopicGroupCoordinatorCannotLeaveException() {
        super("The coordinator cannot leave the group; hand it over to another member first");
    }
}
