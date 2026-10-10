package pl.edu.agh.backend.event.registration;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<EventRegistration> findByEventIdAndUserId(UUID eventId, UUID userId);

    long countByEventIdAndStatus(UUID eventId, EventRegistrationStatus status);

    @Query("select r.status from EventRegistration r where r.event.id = :eventId and r.user.id = :userId")
    Optional<EventRegistrationStatus> findStatus(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    @Query("""
            select count(r)
            from EventRegistration r
            where r.event.id = :eventId
              and r.status = :status
              and (r.registeredAt < :registeredAt or (r.registeredAt = :registeredAt and r.id < :id))
            """)
    long countQueuedAhead(
            @Param("eventId") UUID eventId,
            @Param("status") EventRegistrationStatus status,
            @Param("registeredAt") Instant registeredAt,
            @Param("id") UUID id);

    List<EventRegistration> findByEventIdAndStatusOrderByRegisteredAtAscIdAsc(
            UUID eventId, EventRegistrationStatus status, Limit limit);

    /** An event nobody signed up for has no row here at all, so a missing entry reads as zero. */
    @Query("""
            select r.event.id as eventId, count(r) as seatsTaken
            from EventRegistration r
            where r.event.id in :eventIds and r.status = :status
            group by r.event.id
            """)
    List<EventSeatCount> countByEventIdInAndStatus(
            @Param("eventIds") Collection<UUID> eventIds, @Param("status") EventRegistrationStatus status);

    @Query("""
            select r.event.id as eventId, r.status as status
            from EventRegistration r
            where r.user.id = :userId and r.event.id in :eventIds
            """)
    List<OwnRegistration> findOwnRegistrations(
            @Param("userId") UUID userId, @Param("eventIds") Collection<UUID> eventIds);

    record EventSeatCount(UUID eventId, long seatsTaken) {}

    @Query(value = """
                    select r.* from event_registrations r
                    join events e on e.id = r.event_id
                    where r.reminder_sent_at is null
                      and r.status = 'REGISTERED'
                      and e.deleted_at is null
                      and e.reminder_lead_time_minutes is not null
                      and e.starts_at > now()
                      and e.starts_at - (e.reminder_lead_time_minutes * interval '1 minute') <= now()
                      and r.registered_at <= e.starts_at - (e.reminder_lead_time_minutes * interval '1 minute')
                    """, nativeQuery = true)
    List<EventRegistration> findDueForReminder();

    record OwnRegistration(UUID eventId, EventRegistrationStatus status) {}
}
