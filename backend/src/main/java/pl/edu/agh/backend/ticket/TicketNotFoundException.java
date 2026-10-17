package pl.edu.agh.backend.ticket;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException() {
        super("Ticket not found");
    }
}
