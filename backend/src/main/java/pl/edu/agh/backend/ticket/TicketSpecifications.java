package pl.edu.agh.backend.ticket;

import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class TicketSpecifications {

    public Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public Specification<Ticket> hasCategory(TicketCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }
}
