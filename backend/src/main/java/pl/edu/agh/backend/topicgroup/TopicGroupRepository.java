package pl.edu.agh.backend.topicgroup;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGroupRepository extends JpaRepository<TopicGroup, UUID> {

    /** Feeds {@code SlugUtils.uniqueSlug} when a group is named after an approved request. */
    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "owner")
    Optional<TopicGroup> findWithOwnerById(UUID id);

    @EntityGraph(attributePaths = "owner")
    Page<TopicGroup> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
