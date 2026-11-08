package pl.edu.agh.backend.topicgroup;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;

@Service
@RequiredArgsConstructor
public class AdminTopicGroupService {

    private final TopicGroupRepository topicGroupRepository;
    private final TopicGroupDiscordLinker discordLinker;
    private final DiscordGroupChannels discordGroupChannels;

    @Transactional(readOnly = true)
    public Page<AdminTopicGroupResponse> list(Pageable pageable) {
        boolean discordConfigured = discordGroupChannels.isConfigured();
        return topicGroupRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(group -> AdminTopicGroupResponse.from(group, discordConfigured));
    }

    public AdminTopicGroupResponse linkDiscord(UUID groupId) {
        discordLinker.link(groupId);
        return topicGroupRepository
                .findWithOwnerById(groupId)
                .map(group -> AdminTopicGroupResponse.from(group, discordGroupChannels.isConfigured()))
                .orElseThrow(TopicGroupNotFoundException::new);
    }
}
