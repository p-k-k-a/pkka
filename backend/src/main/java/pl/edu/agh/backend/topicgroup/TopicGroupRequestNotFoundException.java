package pl.edu.agh.backend.topicgroup;

public class TopicGroupRequestNotFoundException extends RuntimeException {

    public TopicGroupRequestNotFoundException() {
        super("Topic group request not found");
    }
}
