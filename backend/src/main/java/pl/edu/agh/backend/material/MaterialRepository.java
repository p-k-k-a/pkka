package pl.edu.agh.backend.material;

import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    @EntityGraph(attributePaths = "event")
    @Override
    java.util.Optional<Material> findById(UUID id);
}
