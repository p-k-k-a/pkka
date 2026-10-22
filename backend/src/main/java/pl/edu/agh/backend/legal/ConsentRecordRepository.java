package pl.edu.agh.backend.legal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    Optional<ConsentRecord> findByUserIdAndDocumentIdAndWithdrawnAtIsNull(UUID userId, UUID documentId);

    /**
     * Records an acceptance unless an active one exists. Done in one statement because a read-then-insert
     * races: two requests both see nothing, and the second insert breaks {@code uq_consent_records_active}
     * and aborts the whole transaction.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
                    insert into consent_records (id, user_id, document_id, source, granted_at)
                    values (gen_random_uuid(), :userId, :documentId, :source, :grantedAt)
                    on conflict (user_id, document_id) where withdrawn_at is null do nothing
                    """, nativeQuery = true)
    void insertIfNoActive(
            @Param("userId") UUID userId,
            @Param("documentId") UUID documentId,
            @Param("source") String source,
            @Param("grantedAt") Instant grantedAt);
}
