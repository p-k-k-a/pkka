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

    @EntityGraph(attributePaths = "author")
    @Override
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    @Override
    Optional<Ticket> findById(UUID id);

    Page<Ticket> findAllByAuthorId(UUID authorId, Pageable pageable);

    Optional<Ticket> findByIdAndAuthorId(UUID id, UUID authorId);
}
