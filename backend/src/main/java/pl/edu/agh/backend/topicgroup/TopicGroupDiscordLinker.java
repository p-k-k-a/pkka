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
        topicGroupRepository.findWithOwnerById(groupId).ifPresent(this::link);
    }

    private void link(TopicGroup group) {
        if (group.getDiscordRoleId() != null && group.getDiscordChannelId() != null) {
            return;
        }
        if (!discordGroupChannels.isConfigured()) {
            log.warn("Discord is not set up for topic groups, {} stays without a role and channel", group.getSlug());
            return;
        }
        Optional<String> ownerDiscordId = Optional.ofNullable(group.getOwner()).map(User::getDiscordId);
        if (group.getDiscordRoleId() == null) {
            Optional<String> roleId = discordGroupChannels.createRole(group.getName());
            if (roleId.isEmpty()) {
                return;
            }
            group.setDiscordRoleId(roleId.get());
            group = topicGroupRepository.save(group);
        }
        Optional<String> channelId = discordGroupChannels.createChannel(group.getSlug(), group.getDiscordRoleId());
        if (channelId.isEmpty()) {
            return;
        }
        group.setDiscordChannelId(channelId.get());
        topicGroupRepository.save(group);
        ownerDiscordId.ifPresent(discordId -> discordGroupChannels.allowManageMessages(channelId.get(), discordId));
    }
}
