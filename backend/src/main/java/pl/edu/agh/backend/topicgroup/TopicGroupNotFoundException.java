package pl.edu.agh.backend.topicgroup;

public class TopicGroupNotFoundException extends RuntimeException {

    public TopicGroupNotFoundException() {
        super("Topic group not found");
    }
}
