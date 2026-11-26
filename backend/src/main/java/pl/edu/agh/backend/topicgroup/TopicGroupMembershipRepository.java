package pl.edu.agh.backend.topicgroup;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TopicGroupMembershipRepository extends JpaRepository<TopicGroupMembership, UUID> {

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    long deleteByGroupIdAndUserId(UUID groupId, UUID userId);

    long countByGroupId(UUID groupId);

    List<TopicGroupMembership> findByUserId(UUID userId);

    @EntityGraph(attributePaths = "user")
    List<TopicGroupMembership> findByGroupIdOrderByJoinedAtAsc(UUID groupId);

    @Query("""
            select m.group.discordRoleId
            from TopicGroupMembership m
            where m.user.id = :userId and m.group.discordRoleId is not null""")
    Set<String> findDiscordRoleIdsByUserId(UUID userId);

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

    /** Per group with a coordinator: members other than the coordinator who joined since its last digest. */
    @Query("""
            select g.id as groupId, g.name as groupName, g.slug as groupSlug,
                   c.id as coordinatorId, c.email as coordinatorEmail, count(m) as newMembers
            from TopicGroupMembership m join m.group g join g.coordinator c
            where m.user.id <> c.id
              and m.joinedAt > coalesce(g.lastJoinDigestAt, g.createdAt)
              and m.joinedAt <= :until
            group by g.id, g.name, g.slug, c.id, c.email""")
    List<JoinDigest> findJoinDigests(Instant until);

    interface JoinDigest {

        UUID getGroupId();

        String getGroupName();

        String getGroupSlug();

        UUID getCoordinatorId();

        String getCoordinatorEmail();

        long getNewMembers();
    }

    interface MemberCount {

        UUID getGroupId();

        long getMembers();
    }
}
