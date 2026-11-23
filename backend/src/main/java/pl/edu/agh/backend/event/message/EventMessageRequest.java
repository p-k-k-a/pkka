package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "A message from an admin to everyone signed up for an event, waitlist included")
public record EventMessageRequest(
        @NotNull @Valid EventMessagePush push,
        @NotNull @Valid EventMessageMail mail) {}
