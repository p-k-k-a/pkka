package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    REJECTED,
}
