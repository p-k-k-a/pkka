package pl.edu.agh.backend.survey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, UUID> {

    boolean existsBySurveyIdAndUserId(UUID surveyId, UUID userId);

    boolean existsBySurveyId(UUID surveyId);

    @EntityGraph(attributePaths = {"answers", "answers.question"})
    List<SurveySubmission> findAllBySurveyId(UUID surveyId);

    Optional<SurveySubmission> findBySurveyIdAndUserId(UUID surveyId, UUID userId);
}
