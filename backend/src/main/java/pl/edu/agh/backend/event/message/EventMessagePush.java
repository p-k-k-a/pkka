package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "The push notification part of an event message")
public record EventMessagePush(
        @Schema(description = "One line: the push title; empty uses the event title", example = "Zmiana sali")
        @Size(max = 100)
        @Pattern(regexp = "[^\\r\\n]*", message = "must be a single line")
        String title,

        @Schema(description = "One line: the push text", example = "Spotkanie przeniesione do sali 2.41")
        @NotBlank
        @Size(max = 150)
        @Pattern(regexp = "[^\\r\\n]*", message = "must be a single line")
        String text) {}
