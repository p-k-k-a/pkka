package pl.edu.agh.backend.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DeviceToken d where d.token = :token and d.installationId <> :installationId")
    void releaseToken(@Param("token") String token, @Param("installationId") String installationId);
}
