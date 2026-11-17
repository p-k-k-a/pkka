package pl.edu.agh.backend.topicgroup;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;
import pl.edu.agh.backend.user.User;

@Component
@RequiredArgsConstructor
@Slf4j
class TopicGroupDiscordLinker {

    private final TopicGroupRepository topicGroupRepository;
    private final DiscordGroupChannels discordGroupChannels;

    @Async
    @TransactionalEventListener
    public void onTopicGroupCreated(TopicGroupCreatedEvent created) {
        link(created.groupId());
    }

    public synchronized void link(UUID groupId) {
        topicGroupRepository.findWithCoordinatorById(groupId).ifPresent(this::link);
    }

    private void link(TopicGroup group) {
        if (!discordGroupChannels.isConfigured()) {
            log.warn("Discord is not set up for topic groups, {} stays without a role and channel", group.getSlug());
            return;
        }
        Optional<String> coordinatorDiscordId =
                Optional.ofNullable(group.getCoordinator()).map(User::getDiscordId);
        if (group.getDiscordRoleId() == null) {
            Optional<String> roleId = discordGroupChannels.createRole(group.getName());
            if (roleId.isEmpty()) {
                return;
            }
            topicGroupRepository.setDiscordRoleId(group.getId(), roleId.get());
            group.setDiscordRoleId(roleId.get());
        }
        if (group.getDiscordChannelId() == null) {
            Optional<String> channelId = discordGroupChannels.createChannel(group.getSlug(), group.getDiscordRoleId());
            if (channelId.isEmpty()) {
                return;
            }
            topicGroupRepository.setDiscordChannelId(group.getId(), channelId.get());
            group.setDiscordChannelId(channelId.get());
        }
        String channelId = group.getDiscordChannelId();
        coordinatorDiscordId.ifPresent(discordId -> discordGroupChannels.allowManageMessages(channelId, discordId));
    }
}
