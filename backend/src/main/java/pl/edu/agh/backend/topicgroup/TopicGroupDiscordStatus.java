package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TopicGroupDiscordStatus {
    LINKED,
    MISSING,
    NOT_CONFIGURED,
}
