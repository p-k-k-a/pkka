package pl.edu.agh.backend.survey;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    @EntityGraph(attributePaths = "questions")
    @Override
    Optional<Survey> findById(UUID id);

    @EntityGraph(attributePaths = "questions")
    Page<Survey> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "questions")
    Page<Survey> findAllByStatusAndEndsAtAfterOrderByEndsAtAsc(SurveyStatus status, Instant now, Pageable pageable);
}
