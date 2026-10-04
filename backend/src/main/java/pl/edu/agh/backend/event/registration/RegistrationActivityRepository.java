package pl.edu.agh.backend.event.registration;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistrationActivityRepository extends JpaRepository<RegistrationActivity, UUID> {

    @Query("""
            select a.type as type, a.occurredAt as occurredAt
            from RegistrationActivity a
            where a.event.id = :eventId
            """)
    List<ActivityOccurrence> findOccurrences(@Param("eventId") UUID eventId);

    /** An event with no activity of some type has no row for it, so a missing entry reads as zero. */
    @Query("""
            select a.event.id as eventId, a.type as type, count(a) as count
            from RegistrationActivity a
            where a.event.id in :eventIds
            group by a.event.id, a.type
            """)
    List<ActivityCount> countByEventIdInGroupedByType(@Param("eventIds") Collection<UUID> eventIds);

    record ActivityOccurrence(RegistrationActivityType type, Instant occurredAt) {}

    record ActivityCount(UUID eventId, RegistrationActivityType type, long count) {}
}
