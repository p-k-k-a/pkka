package pl.edu.agh.backend.topicgroup;

public class TopicGroupRequestAlreadyOpenException extends RuntimeException {

    public TopicGroupRequestAlreadyOpenException() {
        super("User already has a topic group request under review");
    }
}
