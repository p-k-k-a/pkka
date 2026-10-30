package pl.edu.agh.backend.topicgroup;

import java.util.UUID;

public class InvalidTopicGroupRequestStateException extends RuntimeException {

    public InvalidTopicGroupRequestStateException(UUID requestId, TopicGroupRequestStatus current) {
        super("Topic group request %s cannot change state — already %s".formatted(requestId, current));
    }
}
