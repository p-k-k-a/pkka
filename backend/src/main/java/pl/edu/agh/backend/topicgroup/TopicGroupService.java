package pl.edu.agh.backend.topicgroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public Page<TopicGroupResponse> list(Caller caller, Pageable pageable) {
        Page<TopicGroup> groups = topicGroupRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, Long> memberCounts = membershipRepository.memberCounts(
                groups.stream().map(TopicGroup::getId).toList());
        Optional<UUID> callerId = callerUserService.findId(caller);
        Set<UUID> joined = callerId.stream()
                .flatMap(userId -> membershipRepository.findByUserId(userId).stream())
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());
        Optional<String> guildId = guildId();
        return groups.map(group -> toResponse(
                group,
                memberCounts.getOrDefault(group.getId(), 0L),
                callerId,
                joined.contains(group.getId()),
                guildId));
    }

    public TopicGroupResponse get(Caller caller, String slug) {
        TopicGroup group = topicGroupRepository.findBySlug(slug).orElseThrow(TopicGroupNotFoundException::new);
        Optional<UUID> callerId = callerUserService.findId(caller);
        boolean member = callerId.map(userId -> membershipRepository.existsByGroupIdAndUserId(group.getId(), userId))
                .orElse(false);
        return toResponse(group, membershipRepository.countByGroupId(group.getId()), callerId, member, guildId());
    }

    @Transactional
    public void join(Caller caller, UUID groupId) {
        TopicGroup group = topicGroupRepository.findById(groupId).orElseThrow(TopicGroupNotFoundException::new);
        User user = callerUserService.getOrCreate(caller);
        if (!membershipRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            membershipRepository.save(new TopicGroupMembership(group, user));
            eventPublisher.publishEvent(new TopicGroupMembershipChangedEvent(user.getId()));
        }
    }

    @Transactional
    public void leave(Caller caller, UUID groupId) {
        TopicGroup group = topicGroupRepository.findById(groupId).orElseThrow(TopicGroupNotFoundException::new);
        callerUserService.findId(caller).ifPresent(userId -> {
            if (group.isCoordinatedBy(userId)) {
                throw new TopicGroupCoordinatorCannotLeaveException();
            }
            if (membershipRepository.deleteByGroupIdAndUserId(groupId, userId) > 0) {
                eventPublisher.publishEvent(new TopicGroupMembershipChangedEvent(userId));
            }
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
                        membership, group.isCoordinatedBy(membership.getUser().getId())))
                .toList();
    }

    /** Only members get the channel link: the channel is private to the group. */
    private static TopicGroupResponse toResponse(
            TopicGroup group, long memberCount, Optional<UUID> callerId, boolean member, Optional<String> guildId) {
        return TopicGroupResponse.from(
                group,
                memberCount,
                member,
                callerId.filter(group::isCoordinatedBy).isPresent(),
                member ? group.discordChannelUrl(guildId) : null);
    }

    private Optional<String> guildId() {
        return guildSettings.findFirstBy().map(DiscordGuildSettings::getGuildId);
    }
}
