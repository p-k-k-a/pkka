package pl.edu.agh.backend.ticket;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminTicketService {

    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Page<AdminTicketResponse> list(
            Optional<TicketStatus> status, Optional<TicketCategory> category, Pageable pageable) {
        Specification<Ticket> spec = Specification.allOf(
                TicketSpecifications.hasStatus(status.orElse(null)),
                TicketSpecifications.hasCategory(category.orElse(null)));
        return ticketRepository.findAll(spec, pageable).map(AdminTicketResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminTicketResponse get(UUID id) {
        return ticketRepository.findById(id).map(AdminTicketResponse::from).orElseThrow(TicketNotFoundException::new);
    }

    @Transactional
    public AdminTicketResponse update(UUID id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(TicketNotFoundException::new);
        ticket.setStatus(request.status());
        if (request.adminResponse() != null) {
            ticket.setAdminResponse(request.adminResponse());
        }
        return AdminTicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }
}
