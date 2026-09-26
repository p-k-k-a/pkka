package pl.edu.agh.backend.notifications;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByInstallationId(String installationId);

    Optional<DeviceToken> findByToken(String token);

    @Modifying(flushAutomatically = true)
    @Query(value = """
                    insert into device_tokens (id, user_id, installation_id, token, platform, created_at, updated_at)
                    values (gen_random_uuid(), :userId, :installationId, :token, :platform, now(), now())
                    on conflict do nothing
                    """, nativeQuery = true)
    void insertIfAbsent(
            @Param("userId") UUID userId,
            @Param("installationId") String installationId,
            @Param("token") String token,
            @Param("platform") String platform);

    @Modifying(clearAutomatically = true)
    @Query(value = """
                    update device_tokens
                    set user_id = :userId, token = :token, platform = :platform, updated_at = now()
                    where installation_id = :installationId
                    """, nativeQuery = true)
    void updateByInstallationId(
            @Param("userId") UUID userId,
            @Param("installationId") String installationId,
            @Param("token") String token,
            @Param("platform") String platform);

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

    /**
     * Deliberately does not clear the persistence context: this runs inside a caller's transaction, and clearing
     * would detach the entities that caller is midway through updating.
     */
    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("delete from DeviceToken d where d.token in :tokens")
    void deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
