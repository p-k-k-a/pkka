package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
@Tag(name = "Admin Tickets", description = "Ticket handling for administrators")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    @GetMapping
    @Operation(summary = "List all tickets, optionally filtered by status and category")
    public Page<AdminTicketResponse> listAdminTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketCategory category,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return adminTicketService.list(Optional.ofNullable(status), Optional.ofNullable(category), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single ticket")
    @ApiResponse(responseCode = "200", description = "Ticket details")
    @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    public AdminTicketResponse getAdminTicket(@PathVariable UUID id) {
        return adminTicketService.get(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Change a ticket's status and optionally reply to the author")
    @ApiResponse(responseCode = "200", description = "Ticket updated")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "Ticket not found", content = @Content)
    public AdminTicketResponse patchTicket(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        return adminTicketService.update(id, request);
    }
}
