package pl.edu.agh.backend.topicgroup;

import java.util.List;
import java.util.Map;
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
    private final ApplicationEventPublisher eventPublisher;

    public Page<TopicGroupResponse> list(Caller caller, Pageable pageable) {
        Page<TopicGroup> groups = topicGroupRepository.findAllByOrderByCreatedAtDesc(pageable);
        Map<UUID, Long> memberCounts = membershipRepository.memberCounts(
                groups.stream().map(TopicGroup::getId).toList());
        Set<UUID> joined = callerUserService.findId(caller).stream()
                .flatMap(userId -> membershipRepository.findByUserId(userId).stream())
                .map(membership -> membership.getGroup().getId())
                .collect(Collectors.toSet());
        return groups.map(group -> TopicGroupResponse.from(
                group, memberCounts.getOrDefault(group.getId(), 0L), joined.contains(group.getId())));
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
            if (group.getCoordinator() != null
                    && userId.equals(group.getCoordinator().getId())) {
                throw new TopicGroupCoordinatorCannotLeaveException();
            }
            if (membershipRepository.deleteByGroupIdAndUserId(groupId, userId) > 0) {
                eventPublisher.publishEvent(new TopicGroupMembershipChangedEvent(userId));
            }
        });
    }

    public List<TopicGroupMemberResponse> members(Caller caller, UUID groupId) {
        if (!topicGroupRepository.existsById(groupId)) {
            throw new TopicGroupNotFoundException();
        }
        boolean member = callerUserService
                .findId(caller)
                .map(userId -> membershipRepository.existsByGroupIdAndUserId(groupId, userId))
                .orElse(false);
        if (!member && !caller.hasRole(Roles.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only members and admins see who is in the group");
        }
        return membershipRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(TopicGroupMemberResponse::from)
                .toList();
    }
}
