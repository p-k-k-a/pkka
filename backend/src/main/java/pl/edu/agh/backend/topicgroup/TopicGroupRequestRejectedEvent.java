package pl.edu.agh.backend.topicgroup;

import java.util.UUID;

public record TopicGroupRequestRejectedEvent(UUID requestId, UUID requesterId, String groupName, String reason) {}
