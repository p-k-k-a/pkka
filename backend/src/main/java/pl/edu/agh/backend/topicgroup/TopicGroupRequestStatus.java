package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TopicGroupRequestStatus {
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
}
