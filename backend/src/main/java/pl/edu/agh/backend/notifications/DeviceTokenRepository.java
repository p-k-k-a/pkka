package pl.edu.agh.backend.notifications;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    insert into device_tokens (token, user_id, platform, created_at, updated_at)
                    values (:token, :userId, :platform, now(), now())
                    on conflict do nothing
                    """, nativeQuery = true)
    void insertIfAbsent(@Param("token") String token, @Param("userId") UUID userId, @Param("platform") String platform);

    List<DeviceToken> findByUserIdIn(Collection<UUID> userIds);

    @Query("""
            select d from DeviceToken d
            where exists (
                select 1 from Application a
                where a.applicant = d.user
                  and a.status = pl.edu.agh.backend.application.ApplicationStatus.APPROVED
            )
            """)
    List<DeviceToken> findAllForApprovedAlumni();

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("delete from DeviceToken d where d.token in :tokens")
    void deleteByTokenIn(@Param("tokens") Collection<String> tokens);
}
