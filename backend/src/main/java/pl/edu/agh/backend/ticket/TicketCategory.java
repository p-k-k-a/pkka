package pl.edu.agh.backend.ticket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true)
public enum TicketCategory {
    TOPIC_PROPOSAL,
    TECHNICAL_ISSUE,
    OTHER,
}
