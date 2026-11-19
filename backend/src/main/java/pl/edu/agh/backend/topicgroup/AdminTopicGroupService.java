package pl.edu.agh.backend.topicgroup;

import java.util.Map;
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
    private final TopicGroupMembershipRepository membershipRepository;
    private final TopicGroupDiscordLinker discordLinker;
    private final DiscordGroupChannels discordGroupChannels;

    @Transactional(readOnly = true)
    public Page<AdminTopicGroupResponse> list(Pageable pageable) {
        boolean discordConfigured = discordGroupChannels.isConfigured();
        Page<TopicGroup> groups = topicGroupRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, Long> memberCounts = membershipRepository.memberCounts(
                groups.stream().map(TopicGroup::getId).toList());
        return groups.map(group ->
                AdminTopicGroupResponse.from(group, memberCounts.getOrDefault(group.getId(), 0L), discordConfigured));
    }

    public AdminTopicGroupResponse linkDiscord(UUID groupId) {
        discordLinker.link(groupId);
        return topicGroupRepository
                .findWithCoordinatorById(groupId)
                .map(group -> AdminTopicGroupResponse.from(
                        group, membershipRepository.countByGroupId(groupId), discordGroupChannels.isConfigured()))
                .orElseThrow(TopicGroupNotFoundException::new);
    }
}
