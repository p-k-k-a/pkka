package pl.edu.agh.backend.topicgroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGroupRequestRepository extends JpaRepository<TopicGroupRequest, UUID> {

    List<TopicGroupRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    @EntityGraph(attributePaths = {"requester", "createdGroup"})
    Page<TopicGroupRequest> findByStatusOrderByCreatedAtDesc(TopicGroupRequestStatus status, Pageable pageable);

    Optional<TopicGroupRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

    boolean existsByRequesterIdAndStatus(UUID requesterId, TopicGroupRequestStatus status);
}
