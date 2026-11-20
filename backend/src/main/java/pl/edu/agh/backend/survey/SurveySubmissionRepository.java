package pl.edu.agh.backend.survey;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, UUID> {

    boolean existsBySurveyIdAndUserId(UUID surveyId, UUID userId);

    boolean existsBySurveyId(UUID surveyId);

    long countBySurveyId(UUID surveyId);

    /** A survey nobody has answered has no row here at all, so a missing entry reads as zero. */
    @Query("""
            select s.survey.id as surveyId, count(s) as submissionCount
            from SurveySubmission s
            where s.survey.id in :surveyIds
            group by s.survey.id
            """)
    List<SurveySubmissionCount> countSubmissionsBySurveyIdIn(@Param("surveyIds") Collection<UUID> surveyIds);

    record SurveySubmissionCount(UUID surveyId, long submissionCount) {}

    @EntityGraph(attributePaths = {"answers", "answers.question"})
    List<SurveySubmission> findAllBySurveyId(UUID surveyId);

    Optional<SurveySubmission> findBySurveyIdAndUserId(UUID surveyId, UUID userId);
}
