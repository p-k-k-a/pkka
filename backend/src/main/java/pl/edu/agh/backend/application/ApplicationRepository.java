package pl.edu.agh.backend.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findFirstByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    Optional<Application> findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(
            UUID applicantId, ApplicationStatus status);

    @EntityGraph(attributePaths = "applicant")
    Page<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    boolean existsByApplicantIdAndStatusIn(UUID applicantId, List<ApplicationStatus> statuses);

    boolean existsByApplicantKeycloakIdAndStatus(String keycloakId, ApplicationStatus status);

    @Query(
            "select distinct a.applicant.id from Application a where a.status = :status and a.applicant.id in :applicantIds")
    Set<UUID> findApplicantIdsByStatus(Collection<UUID> applicantIds, ApplicationStatus status);
}
