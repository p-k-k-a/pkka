package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "A message from an admin to everyone signed up for an event, waitlist included")
public record EventMessageRequest(
        @Schema(description = "Mail subject and push title", example = "Zmiana sali") @NotBlank @Size(max = 150)
        String subject,

        @Schema(description = "Plain text; line breaks are kept", example = "Spotykamy się w sali 2.41 zamiast 1.38.")
        @NotBlank
        @Size(max = 5000)
        String body) {}
