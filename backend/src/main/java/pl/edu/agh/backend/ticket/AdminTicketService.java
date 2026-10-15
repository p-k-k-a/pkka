package pl.edu.agh.backend.ticket;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;

@Service
@RequiredArgsConstructor
public class AdminTicketService {

    private final TicketRepository ticketRepository;
    private final CallerUserService callerUserService;

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
    public AdminTicketResponse update(Caller caller, UUID id, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(TicketNotFoundException::new);
        ticket.setStatus(request.status());
        if (request.adminResponse() != null) {
            ticket.setAdminResponse(request.adminResponse());
            ticket.setRespondedBy(callerUserService.getOrCreate(caller));
        }
        return AdminTicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }
}
