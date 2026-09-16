package pl.edu.agh.backend.survey;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    // Fetch-joining the questions collection is safe here: findById loads a single row, so there
    // is no pagination to defeat by multiplying rows client-side.
    @EntityGraph(attributePaths = "questions")
    @Override
    Optional<Survey> findById(UUID id);

    // Deliberately no @EntityGraph on the collection here: a fetch-join would multiply rows and
    // force Hibernate to paginate in memory. Survey.questions uses @BatchSize instead, so the
    // page of surveys is still selected with a real SQL LIMIT/OFFSET, and questions are then
    // batch-loaded for just that page.
    Page<Survey> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Survey> findAllByStatusAndEndsAtAfterOrderByEndsAtAsc(SurveyStatus status, Instant now, Pageable pageable);
}
