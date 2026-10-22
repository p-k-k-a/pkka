package pl.edu.agh.backend.legal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    @Query("""
            select c from ConsentRecord c
            join fetch c.document
            where c.user.id = :userId and c.withdrawnAt is null
            """)
    List<ConsentRecord> findActiveByUserId(@Param("userId") UUID userId);

    boolean existsByUserIdAndDocumentIdAndWithdrawnAtIsNull(UUID userId, UUID documentId);
}
