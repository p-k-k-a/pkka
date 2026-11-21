package pl.edu.agh.backend.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    @Query("""
            select u from User u
            where exists (
                select 1 from Application a
                where a.applicant = u
                  and a.status = pl.edu.agh.backend.application.ApplicationStatus.APPROVED
            )
            """)
    List<User> findApprovedAlumni();

    Optional<User> findByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsById(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    void deleteNow(@Param("id") UUID id);
}
