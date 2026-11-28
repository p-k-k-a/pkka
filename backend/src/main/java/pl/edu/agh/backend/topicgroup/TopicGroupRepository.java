package pl.edu.agh.backend.topicgroup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TopicGroupRepository extends JpaRepository<TopicGroup, UUID> {

    /** Feeds {@code SlugUtils.uniqueSlug} when a group is named after an approved request. */
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "coordinator")
    Optional<TopicGroup> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "coordinator")
    Optional<TopicGroup> findWithCoordinatorById(UUID id);

    @EntityGraph(attributePaths = "coordinator")
    Page<TopicGroup> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<TopicGroup> findByCoordinatorIdAndDiscordChannelIdNotNull(UUID coordinatorId);

    @Query("select g.discordRoleId from TopicGroup g where g.discordRoleId is not null")
    Set<String> findDiscordRoleIds();

    @Transactional
    @Modifying
    @Query("update TopicGroup g set g.discordRoleId = :roleId where g.id = :id")
    void setDiscordRoleId(@Param("id") UUID id, @Param("roleId") String roleId);

    @Transactional
    @Modifying
    @Query("update TopicGroup g set g.lastJoinDigestAt = :until where g.id = :id")
    void setLastJoinDigestAt(@Param("id") UUID id, @Param("until") Instant until);

    @Transactional
    @Modifying
    @Query("update TopicGroup g set g.discordChannelId = :channelId where g.id = :id")
    void setDiscordChannelId(@Param("id") UUID id, @Param("channelId") String channelId);
}
