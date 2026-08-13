package pl.edu.agh.backend.event;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @EntityGraph(attributePaths = "tags")
    @Override
    @NullMarked
    Optional<Event> findById(UUID id);

    /**
     * {@code SELECT ... FOR UPDATE} — the serialization point for seat booking. Tags are not fetched
     * here; booking a seat only needs the event row itself.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Event> findForUpdateById(UUID id);
}
