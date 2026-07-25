package pl.edu.agh.backend.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsByKeycloakId(String keycloakId);
}
