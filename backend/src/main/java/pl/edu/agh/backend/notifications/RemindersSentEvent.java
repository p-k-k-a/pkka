package pl.edu.agh.backend.notifications;

import java.util.List;
import java.util.UUID;

/** The people whose reminder for an event was just recorded as sent. */
public record RemindersSentEvent(UUID eventId, List<UUID> userIds) {}
