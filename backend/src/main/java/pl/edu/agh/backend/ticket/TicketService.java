package pl.edu.agh.backend.ticket;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CallerUserService callerUserService;

    @Transactional
    public TicketResponse submit(Caller caller, CreateTicketRequest request) {
        User author = callerUserService.getOrCreate(caller);
        Ticket ticket = new Ticket(author, request.category(), request.title(), request.description());
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }

    /**
     * A verified-alumn token with no local {@link User} row yet (provisioned lazily on first write
     * via {@link CallerUserService#getOrCreate}) is authenticated and authorized — they simply have
     * zero tickets, so the result is an empty page rather than 401/404.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponse> listMine(Caller caller, Pageable pageable) {
        return callerUserService
                .findId(caller)
                .map(authorId ->
                        ticketRepository.findAllByAuthorId(authorId, pageable).map(TicketResponse::from))
                .orElseGet(() -> Page.empty(pageable));
    }

    /** Someone else's ticket is reported as missing, so ids can't be probed for existence. */
    @Transactional(readOnly = true)
    public TicketResponse getMine(Caller caller, UUID id) {
        return callerUserService
                .findId(caller)
                .flatMap(authorId -> ticketRepository.findByIdAndAuthorId(id, authorId))
                .map(TicketResponse::from)
                .orElseThrow(TicketNotFoundException::new);
    }
}
