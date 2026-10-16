package pl.edu.agh.backend.event;

import java.util.UUID;

public record EventDeletedEvent(UUID eventId) {}
