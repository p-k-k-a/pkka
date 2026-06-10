package pl.edu.agh.backend.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findFirstByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    @EntityGraph(attributePaths = "applicant")
    Page<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    boolean existsByApplicantIdAndStatusIn(UUID applicantId, List<ApplicationStatus> statuses);
}
