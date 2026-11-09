package pl.edu.agh.backend.ticket;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    // Both users are to-one associations, so fetch-joining them keeps the SQL LIMIT/OFFSET of the
    // page and avoids a query per row when the DTO reads the display names.
    @EntityGraph(attributePaths = {"author", "respondedBy"})
    @Override
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "respondedBy"})
    @Override
    Optional<Ticket> findById(UUID id);

    @EntityGraph(attributePaths = "respondedBy")
    Page<Ticket> findAllByAuthorId(UUID authorId, Pageable pageable);

    @EntityGraph(attributePaths = "respondedBy")
    Optional<Ticket> findByIdAndAuthorId(UUID id, UUID authorId);
}
