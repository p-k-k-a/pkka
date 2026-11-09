package pl.edu.agh.backend.notifications;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

@Entity
@Table(name = "push_tickets")
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PushTicket {

    @Id
    @Column(name = "ticket_id", nullable = false, length = 64)
    private String ticketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "token", nullable = false)
    private DeviceToken device;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PushTicket other)) {
            return false;
        }
        return ticketId != null && ticketId.equals(other.ticketId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
