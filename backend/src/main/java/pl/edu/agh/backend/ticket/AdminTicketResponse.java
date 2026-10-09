package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminTicketResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) TicketCategory category,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) TicketStatus status,
        String adminResponse,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID authorId,
        @Schema(requiredMode = RequiredMode.REQUIRED) String authorDisplayName,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminTicketResponse from(Ticket ticket) {
        return new AdminTicketResponse(
                ticket.getId(),
                ticket.getCategory(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAdminResponse(),
                ticket.getAuthor().getId(),
                ticket.getAuthor().getDisplayName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
