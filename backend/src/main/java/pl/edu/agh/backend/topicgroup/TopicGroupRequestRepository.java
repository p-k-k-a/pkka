package pl.edu.agh.backend.topicgroup;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicGroupRequestRepository extends JpaRepository<TopicGroupRequest, UUID> {

    List<TopicGroupRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    @EntityGraph(attributePaths = "requester")
    Page<TopicGroupRequest> findByStatusOrderByCreatedAtDesc(TopicGroupRequestStatus status, Pageable pageable);

    boolean existsByRequesterIdAndStatus(UUID requesterId, TopicGroupRequestStatus status);
}
