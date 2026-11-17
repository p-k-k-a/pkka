package pl.edu.agh.backend.topicgroup;

public class TopicGroupNameTakenException extends RuntimeException {

    public TopicGroupNameTakenException(String name) {
        super("A topic group named '%s' already exists".formatted(name));
    }
}
