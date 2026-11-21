package pl.edu.agh.backend.discord;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record DiscordStatusResponse(
        @Schema(
                description = "UNKNOWN when Discord could not be asked; show no banner then",
                requiredMode = RequiredMode.REQUIRED)
        DiscordStatus status,

        @Schema(description = "Invite to the club's server, only when the caller is not on it")
        String inviteUrl) {}
