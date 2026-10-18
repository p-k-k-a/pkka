package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Tickets submitted by verified alumni")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new ticket")
    @ApiResponse(responseCode = "201", description = "Ticket created")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request, Caller caller) {
        return ticketService.submit(caller, request);
    }

    @GetMapping
    @Operation(summary = "List tickets submitted by the current user")
    public Page<TicketResponse> listMyTickets(
            Caller caller,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return ticketService.listMine(caller, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the current user's tickets")
    @ApiResponse(responseCode = "200", description = "Ticket details")
    @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    public TicketResponse getMyTicket(@PathVariable UUID id, Caller caller) {
        return ticketService.getMine(caller, id);
    }
}
