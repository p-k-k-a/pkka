package pl.edu.agh.backend.topic;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicProposalRepository extends JpaRepository<TopicProposal, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<TopicProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<TopicProposal> findAllByStatusOrderByCreatedAtDesc(TopicProposalStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<TopicProposal> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Override
    java.util.Optional<TopicProposal> findById(UUID id);
}
