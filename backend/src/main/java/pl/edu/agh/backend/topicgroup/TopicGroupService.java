package pl.edu.agh.backend.topicgroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettings;
import pl.edu.agh.backend.infrastructure.discord.DiscordGuildSettingsRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicGroupService {

    private final TopicGroupRepository topicGroupRepository;
    private final TopicGroupMembershipRepository membershipRepository;
    private final CallerUserService callerUserService;
    private final DiscordGuildSettingsRepository guildSettings;

    public Page<TopicGroupResponse> list(Caller caller, Pageable pageable) {
        Page<TopicGroup> groups = topicGroupRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, Long> memberCounts = membershipRepository.memberCounts(
                groups.stream().map(TopicGroup::getId).toList());
        Optional<UUID> callerId = callerUserService.findId(caller);
        Set<UUID> joined = callerId.stream()
                .flatMap(userId -> membershipRepository.findByUserId(userId).stream())
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());
        Optional<String> guildId = guildSettings.findFirstBy().map(DiscordGuildSettings::getGuildId);
        return groups.map(group -> {
            boolean member = joined.contains(group.getId());
            return TopicGroupResponse.from(
                    group,
                    memberCounts.getOrDefault(group.getId(), 0L),
                    member,
                    callerId.filter(userId -> coordinates(group, userId)).isPresent(),
                    member ? discordChannelUrl(group, guildId) : null);
        });
    }

    @Transactional
    public void join(Caller caller, UUID groupId) {
        TopicGroup group = topicGroupRepository.findById(groupId).orElseThrow(TopicGroupNotFoundException::new);
        User user = callerUserService.getOrCreate(caller);
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            membershipRepository.save(new TopicGroupMembership(group, user));
        }
    }

    @Transactional
    public void leave(Caller caller, UUID groupId) {
        TopicGroup group = topicGroupRepository.findById(groupId).orElseThrow(TopicGroupNotFoundException::new);
        callerUserService.findId(caller).ifPresent(userId -> {
            if (coordinates(group, userId)) {
                throw new TopicGroupCoordinatorCannotLeaveException();
            }
            membershipRepository.deleteByGroupIdAndUserId(groupId, userId);
        });
    }

    public List<TopicGroupMemberResponse> members(Caller caller, UUID groupId) {
        TopicGroup group = topicGroupRepository.findById(groupId).orElseThrow(TopicGroupNotFoundException::new);
        boolean member = callerUserService
                .findId(caller)
                .map(userId -> membershipRepository.existsByGroupIdAndUserId(groupId, userId))
                .orElse(false);
        if (!member && !caller.hasRole(Roles.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only members and admins see who is in the group");
        }
        return membershipRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(membership -> TopicGroupMemberResponse.from(
                        membership, coordinates(group, membership.getUser().getId())))
                .toList();
    }

    private static boolean coordinates(TopicGroup group, UUID userId) {
        return group.getCoordinator() != null
                && userId.equals(group.getCoordinator().getId());
    }

    private static String discordChannelUrl(TopicGroup group, Optional<String> guildId) {
        if (group.getDiscordChannelId() == null) {
            return null;
        }
        return guildId.map(id -> "https://discord.com/channels/%s/%s".formatted(id, group.getDiscordChannelId()))
                .orElse(null);
    }
}
