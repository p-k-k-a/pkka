package pl.edu.agh.backend.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByKeycloakId(String keycloakId);

    @EntityGraph(attributePaths = "tags")
    Optional<User> findWithTagsByKeycloakId(String keycloakId);
}
