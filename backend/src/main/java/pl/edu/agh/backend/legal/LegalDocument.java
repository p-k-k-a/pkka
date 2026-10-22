package pl.edu.agh.backend.legal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

/**
 * One published version of a legal document. Versions are immutable: consent records point at them as
 * proof of the accepted text, so a change is always a new version.
 */
@Entity
@Table(
        name = "legal_documents",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_legal_documents_version",
                        columnNames = {"type", "version"}))
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LegalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 40)
    @ToString.Include
    private LegalDocumentType type;

    @Column(name = "version", nullable = false, updatable = false)
    @ToString.Include
    private int version;

    @Column(name = "title", nullable = false, updatable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "change_summary", updatable = false, columnDefinition = "TEXT")
    private String changeSummary;

    /** Whether people who accepted an earlier version must accept this one before using the app again. */
    @Column(name = "requires_reacceptance", nullable = false, updatable = false)
    private boolean requiresReacceptance;

    /** Also the moment the version takes effect; a future date keeps the previous version current. */
    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LegalDocument other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
