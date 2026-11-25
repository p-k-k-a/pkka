package pl.edu.agh.backend.topicgroup;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.infrastructure.discord.DiscordGroupChannels;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettings;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettingsRepository;

@Service
@RequiredArgsConstructor
public class AdminTopicGroupService {

    private final TopicGroupRepository topicGroupRepository;
    private final TopicGroupMembershipRepository membershipRepository;
    private final TopicGroupDiscordLinker discordLinker;
    private final DiscordGroupChannels discordGroupChannels;
    private final DiscordGuildSettingsRepository guildSettings;

    @Transactional(readOnly = true)
    public Page<AdminTopicGroupResponse> list(Pageable pageable) {
        boolean discordConfigured = discordGroupChannels.isConfigured();
        Page<TopicGroup> groups = topicGroupRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, Long> memberCounts = membershipRepository.memberCounts(
                groups.stream().map(TopicGroup::getId).toList());
        Optional<String> guildId = guildId();
        return groups.map(group -> AdminTopicGroupResponse.from(
                group,
                memberCounts.getOrDefault(group.getId(), 0L),
                discordConfigured,
                TopicGroupService.discordChannelUrl(group, guildId)));
    }

    public AdminTopicGroupResponse linkDiscord(UUID groupId) {
        discordLinker.link(groupId);
        return topicGroupRepository
                .findWithCoordinatorById(groupId)
                .map(group -> AdminTopicGroupResponse.from(
                        group,
                        membershipRepository.countByGroupId(groupId),
                        discordGroupChannels.isConfigured(),
                        TopicGroupService.discordChannelUrl(group, guildId())))
                .orElseThrow(TopicGroupNotFoundException::new);
    }

    private Optional<String> guildId() {
        return guildSettings.findFirstBy().map(DiscordGuildSettings::getGuildId);
    }
}
