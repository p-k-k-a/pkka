package pl.edu.agh.backend.repository;

import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pl.edu.agh.backend.domain.Event;

public interface EventRepository
        extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = "tags")
    @Override
    @NullMarked
    Optional<Event> findById(UUID id);
}
