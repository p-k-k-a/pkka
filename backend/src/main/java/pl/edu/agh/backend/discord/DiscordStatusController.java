package pl.edu.agh.backend.discord;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/me/discord")
@RequiredArgsConstructor
@Tag(name = "Me", description = "Current user identity and roles")
public class DiscordStatusController {

    private final DiscordStatusService discordStatusService;

    @GetMapping
    @Operation(
            summary = "Whether the caller's Discord is linked and on the club's server",
            description = "Also brings the caller's Discord roles up to date, at most once a minute.")
    public DiscordStatusResponse getMyDiscordStatus(Caller caller) {
        return discordStatusService.status(caller);
    }
}
