package pl.edu.agh.backend.event;

import java.time.Instant;
import java.util.UUID;

/**
 * An upcoming event was deleted, which for the people signed up means it is cancelled. Carries what the
 * message needs, since the soft-deleted event can no longer be loaded.
 */
public record EventCancelledEvent(UUID eventId, String title, String slug, Instant startsAt) {}
