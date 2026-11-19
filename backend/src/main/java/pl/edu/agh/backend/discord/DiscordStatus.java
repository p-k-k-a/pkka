package pl.edu.agh.backend.discord;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum DiscordStatus {
    NOT_LINKED,
    NOT_ON_SERVER,
    ON_SERVER
}
