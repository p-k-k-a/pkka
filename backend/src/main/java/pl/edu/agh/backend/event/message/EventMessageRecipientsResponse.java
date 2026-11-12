package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.media.Schema;

public record EventMessageRecipientsResponse(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                description = "People signed up for the event, waitlist included, who will get the message")
        long recipients) {}
