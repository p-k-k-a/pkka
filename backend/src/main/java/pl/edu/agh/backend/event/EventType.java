package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum EventType {
    ONLINE,
    IN_PERSON,
    HYBRID
}
