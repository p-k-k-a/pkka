package pl.edu.agh.backend.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByInstallationId(String installationId);

    /**
     * Bulk delete so the row is gone before the caller inserts its own; a queued delete would flush after the
     * insert and trip the unique constraint on {@code token}.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DeviceToken d where d.token = :token and d.installationId <> :installationId")
    void releaseToken(@Param("token") String token, @Param("installationId") String installationId);

    List<DeviceToken> findByUserIdIn(Collection<UUID> userIds);

    /** An approved application is the database-side proof of alumn-hood; the role itself lives only in Keycloak. */
    @Query("""
            select d from DeviceToken d
            where exists (
                select 1 from Application a
                where a.applicant = d.user
                  and a.status = pl.edu.agh.backend.application.ApplicationStatus.APPROVED
            )
            """)
    List<DeviceToken> findAllForApprovedAlumni();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DeviceToken d where d.token in :tokens")
    void deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
