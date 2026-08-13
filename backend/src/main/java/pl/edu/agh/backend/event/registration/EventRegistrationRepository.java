package pl.edu.agh.backend.event.registration;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<EventRegistration> findByEventIdAndUserId(UUID eventId, UUID userId);

    long countByEventId(UUID eventId);

    /** An event nobody signed up for has no row here at all, so a missing entry reads as zero. */
    @Query("""
            select r.event.id as eventId, count(r) as seatsTaken
            from EventRegistration r
            where r.event.id in :eventIds
            group by r.event.id
            """)
    List<EventSeatCount> countByEventIdIn(@Param("eventIds") Collection<UUID> eventIds);

    interface EventSeatCount {
        UUID getEventId();

        long getSeatsTaken();
    }
}
