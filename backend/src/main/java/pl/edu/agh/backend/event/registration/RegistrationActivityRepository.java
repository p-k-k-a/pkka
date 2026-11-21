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
            select a.type as type, a.source as source, a.occurredAt as occurredAt
            from RegistrationActivity a
            where a.event.id = :eventId
            """)
    List<ActivityOccurrence> findOccurrences(@Param("eventId") UUID eventId);

    @Query("""
            select a.event.id as eventId, count(a) as count
            from RegistrationActivity a
            where a.event.id in :eventIds
              and a.type = pl.edu.agh.backend.event.registration.RegistrationActivityType.CANCELLED
            group by a.event.id
            """)
    List<ActivityCount> countCancellationsByEventIdIn(@Param("eventIds") Collection<UUID> eventIds);

    /** Sign-ups recorded before sources were have none and are left out. */
    @Query("""
            select a.event.id as eventId, a.source as source, count(a) as count
            from RegistrationActivity a
            where a.event.id in :eventIds
              and a.type in (
                  pl.edu.agh.backend.event.registration.RegistrationActivityType.SIGNED_UP,
                  pl.edu.agh.backend.event.registration.RegistrationActivityType.WAITLISTED)
              and a.source is not null
            group by a.event.id, a.source
            """)
    List<SourceCount> countSignUpsBySourceByEventIdIn(@Param("eventIds") Collection<UUID> eventIds);

    record ActivityOccurrence(RegistrationActivityType type, RegistrationSource source, Instant occurredAt) {}

    record ActivityCount(UUID eventId, long count) {}

    record SourceCount(UUID eventId, RegistrationSource source, long count) {}
}
