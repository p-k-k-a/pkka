package pl.edu.agh.backend.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByApplicantIdOrderByCreatedAtDesc(UUID applicantId);

    @EntityGraph(attributePaths = "applicant")
    Page<Application> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    boolean existsByApplicantIdAndStatusIn(UUID applicantId, List<ApplicationStatus> statuses);
}
