package pl.edu.agh.backend.survey;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveyRepository extends JpaRepository<Survey, UUID> {

    // Fetch-joining the questions collection is safe here: findById loads a single row, so there
    // is no pagination to defeat by multiplying rows client-side.
    @EntityGraph(attributePaths = "questions")
    @Override
    Optional<Survey> findById(UUID id);

    /**
     * {@code SELECT ... FOR UPDATE} — mirrors {@code EventRepository.findForUpdateById}. Submitting
     * an answer and editing/deleting a survey are both check-then-act (submission existence,
     * "no submissions yet"), so both paths lock this row to serialize against each other.
     */
    @EntityGraph(attributePaths = "questions")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Survey> findForUpdateById(UUID id);

    // Survey lists don't need the questions, so there is deliberately no @EntityGraph here and
    // callers must not touch Survey.questions for the rows of a page (use countQuestionsBySurveyIdIn
    // when a list needs the number of questions).
    Page<Survey> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Survey> findAllByStatusAndEndsAtAfterOrderByEndsAtAsc(SurveyStatus status, Instant now, Pageable pageable);

    /** A survey without questions has no row here at all, so a missing entry reads as zero. */
    @Query("""
            select q.survey.id as surveyId, count(q) as questionCount
            from SurveyQuestion q
            where q.survey.id in :surveyIds
            group by q.survey.id
            """)
    List<SurveyQuestionCount> countQuestionsBySurveyIdIn(@Param("surveyIds") Collection<UUID> surveyIds);

    record SurveyQuestionCount(UUID surveyId, long questionCount) {}
}
