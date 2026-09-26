package pl.edu.agh.backend.notifications;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTicketRepository extends JpaRepository<PushTicket, String> {

    List<PushTicket> findByCreatedAtBefore(Instant cutoff);

    @Modifying
    @Query("delete from PushTicket t where t.createdAt < :cutoff")
    void deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
