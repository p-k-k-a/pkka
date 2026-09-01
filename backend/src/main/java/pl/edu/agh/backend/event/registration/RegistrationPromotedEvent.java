package pl.edu.agh.backend.event.registration;

import java.util.UUID;

public record RegistrationPromotedEvent(UUID eventId, UUID userId) {}
