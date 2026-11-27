package pl.edu.agh.backend.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClientException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildMembership;
import pl.edu.agh.backend.topicgroup.TopicGroupMembershipChangedEvent;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiscordRoleSyncListener {

    private final DiscordGuildMembership guildMembership;
    private final DiscordRoleSync roleSync;
    private final UserRepository userRepository;

    @TransactionalEventListener
    public void onTopicGroupMembershipChanged(TopicGroupMembershipChangedEvent event) {
        if (!guildMembership.isConfigured()) {
            return;
        }
        userRepository.findById(event.userId()).ifPresent(this::sync);
    }

    private void sync(User user) {
        try {
            roleSync.sync(user);
        } catch (RestClientException ex) {
            log.warn("Could not sync the Discord roles of user {}: {}", user.getId(), ex.getMessage());
        }
    }
}
