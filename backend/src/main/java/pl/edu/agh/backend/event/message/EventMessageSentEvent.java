package pl.edu.agh.backend.event.message;

import java.util.UUID;

/** An admin's message to an event's sign-ups, delivered after commit. */
public record EventMessageSentEvent(
        UUID eventId, String pushTitle, String pushText, String mailSubject, String mailBody) {}
