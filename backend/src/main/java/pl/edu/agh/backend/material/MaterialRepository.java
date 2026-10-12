package pl.edu.agh.backend.material;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MaterialRepository extends JpaRepository<Material, UUID>, JpaSpecificationExecutor<Material> {

    @EntityGraph(attributePaths = "event")
    @Override
    Optional<Material> findById(UUID id);

    /**
     * {@code event} is a to-one association, so fetch-joining it here does not multiply result
     * rows the way a collection join would; unlike the survey question/option collections (see
     * SurveyRepository), this fetch join is safe to combine with {@link Pageable} because
     * pagination still happens with a real SQL LIMIT/OFFSET on the {@code materials} table.
     */
    @EntityGraph(attributePaths = "event")
    @Override
    Page<Material> findAll(Specification<Material> spec, Pageable pageable);
}
