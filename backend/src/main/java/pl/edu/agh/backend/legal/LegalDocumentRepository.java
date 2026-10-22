package pl.edu.agh.backend.legal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    /** The version in force at {@code now}: the highest one already published. */
    Optional<LegalDocument> findFirstByTypeAndPublishedAtLessThanEqualOrderByVersionDesc(
            LegalDocumentType type, Instant now);

    default Optional<LegalDocument> findCurrent(LegalDocumentType type, Instant now) {
        return findFirstByTypeAndPublishedAtLessThanEqualOrderByVersionDesc(type, now);
    }

    @Query("""
            select d from LegalDocument d
            where d.publishedAt <= :now
              and d.version = (
                  select max(newer.version) from LegalDocument newer
                  where newer.type = d.type and newer.publishedAt <= :now)
            """)
    List<LegalDocument> findAllCurrent(@Param("now") Instant now);
}
