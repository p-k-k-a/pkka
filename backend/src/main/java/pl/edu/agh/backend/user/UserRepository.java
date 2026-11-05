package pl.edu.agh.backend.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsById(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from User u where u.id = :id")
    void deleteNow(@Param("id") UUID id);
}
