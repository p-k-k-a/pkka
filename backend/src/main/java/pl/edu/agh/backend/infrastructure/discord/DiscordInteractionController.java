package pl.edu.agh.backend.infrastructure.discord;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Hidden
@RestController
@RequiredArgsConstructor
public class DiscordInteractionController {

    private static final int PING = 1;
    private static final int APPLICATION_COMMAND = 2;
    private static final int PONG = 1;

    private final DiscordSignature signature;
    private final DiscordSetupCommand setupCommand;
    private final JsonMapper jsonMapper;

    @PostMapping("/api/public/discord/interactions")
    public ResponseEntity<Map<String, Object>> interact(
            @RequestHeader(name = "X-Signature-Ed25519", required = false) String signatureHex,
            @RequestHeader(name = "X-Signature-Timestamp", required = false) String timestamp,
            @RequestBody byte[] body) {
        if (!signature.verify(timestamp, body, signatureHex)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        JsonNode interaction = jsonMapper.readTree(body);
        return switch (interaction.path("type").asInt()) {
            case PING -> ResponseEntity.ok(Map.of("type", PONG));
            case APPLICATION_COMMAND -> ResponseEntity.ok(setupCommand.handle(interaction));
            default -> ResponseEntity.badRequest().build();
        };
    }
}
