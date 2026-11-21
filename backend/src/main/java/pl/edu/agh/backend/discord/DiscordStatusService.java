package pl.edu.agh.backend.discord;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.infrastructure.discord.DiscordInvites;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordStatusService {

    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(1);

    private final CallerUserService callerUserService;
    private final DiscordGuildMembership guildMembership;
    private final DiscordRoleSync roleSync;
    private final DiscordInvites invites;
    private final Map<UUID, Check> lastChecks = new ConcurrentHashMap<>();

    public DiscordStatusResponse status(Caller caller) {
        DiscordStatus status = callerUserService.find(caller).map(this::status).orElse(DiscordStatus.NOT_LINKED);
        String inviteUrl =
                status == DiscordStatus.NOT_ON_SERVER ? invites.inviteUrl().orElse(null) : null;
        return new DiscordStatusResponse(status, inviteUrl);
    }

    private DiscordStatus status(User user) {
        if (user.getDiscordId() == null) {
            return DiscordStatus.NOT_LINKED;
        }
        Check last = lastChecks.get(user.getId());
        if (last != null && last.at().plus(CHECK_INTERVAL).isAfter(Instant.now())) {
            return last.status();
        }
        DiscordStatus status = check(user);
        lastChecks.put(user.getId(), new Check(status, Instant.now()));
        return status;
    }

    private DiscordStatus check(User user) {
        if (!guildMembership.isConfigured()) {
            return DiscordStatus.UNKNOWN;
        }
        try {
            return roleSync.sync(user);
        } catch (RestClientException ex) {
            log.warn("Could not check Discord for user {}: {}", user.getId(), ex.getMessage());
            return DiscordStatus.UNKNOWN;
        }
    }

    private record Check(DiscordStatus status, Instant at) {}
}
