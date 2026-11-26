package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "The mail part of an event message")
public record EventMessageMail(
        @Schema(description = "One line: the mail subject", example = "Zmiana sali")
        @NotBlank
        @Size(max = 150)
        @Pattern(regexp = "[^\\r\\n]*", message = "must be a single line")
        String subject,

        @Schema(description = "Plain text; line breaks are kept", example = "Spotykamy się w sali 2.41 zamiast 1.38.")
        @NotBlank
        @Size(max = 5000)
        String body) {}
