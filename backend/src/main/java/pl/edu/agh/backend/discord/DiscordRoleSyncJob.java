package pl.edu.agh.backend.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.infrastructure.discord.DiscordRateLimitedException;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordRoleSyncJob {

    private final DiscordGuildMembership guildMembership;
    private final DiscordRoleSync roleSync;
    private final UserRepository userRepository;

    @Scheduled(cron = "${app.discord.role-sync-cron:0 */15 * * * *}")
    public void syncAll() throws InterruptedException {
        if (!guildMembership.isConfigured()) {
            return;
        }
        for (User user : userRepository.findByDiscordIdIsNotNull()) {
            sync(user, true);
        }
    }

    private void sync(User user, boolean retryWhenRateLimited) throws InterruptedException {
        try {
            roleSync.sync(user);
        } catch (DiscordRateLimitedException ex) {
            if (retryWhenRateLimited) {
                Thread.sleep(ex.getRetryAfter());
                sync(user, false);
            } else {
                log.warn("Discord is still rate limiting, skipping the roles of user {}", user.getId());
            }
        } catch (RestClientException ex) {
            log.warn("Could not sync the Discord roles of user {}: {}", user.getId(), ex.getMessage());
        }
    }
}
