package pl.edu.agh.backend.topicgroup;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicGroupMembershipRepository extends JpaRepository<TopicGroupMembership, UUID> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    List<TopicGroupMembership> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<TopicGroupMembership> findByGroupIdOrderByJoinedAtAsc(UUID groupId);

    @Query("""
            select m.group.id as groupId, count(m) as members
            from TopicGroupMembership m
            where m.group.id in :groupIds
            group by m.group.id""")
    List<MemberCount> countByGroupIds(Collection<UUID> groupIds);

    default Map<UUID, Long> memberCounts(Collection<UUID> groupIds) {
        return countByGroupIds(groupIds).stream()
                .collect(Collectors.toMap(MemberCount::getGroupId, MemberCount::getMembers));
    }

    interface MemberCount {

        UUID getGroupId();

        long getMembers();
    }
}
