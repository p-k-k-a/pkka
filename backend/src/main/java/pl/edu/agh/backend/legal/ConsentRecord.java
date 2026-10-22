package pl.edu.agh.backend.legal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import pl.edu.agh.backend.user.User;

/**
 * A user's acceptance of one document version. Withdrawing keeps the row, since it is the proof of consent;
 * at most one row per user and version is active ({@code uq_consent_records_active}).
 */
@Entity
@Table(name = "consent_records", indexes = @Index(name = "idx_consent_records_user", columnList = "user_id"))
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private LegalDocument document;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, updatable = false, length = 20)
    @ToString.Include
    private ConsentSource source;

    @Column(name = "granted_at", nullable = false, updatable = false)
    @ToString.Include
    private Instant grantedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    public boolean isActive() {
        return withdrawnAt == null;
    }

    public void withdraw(Instant at) {
        if (isActive()) {
            withdrawnAt = at;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConsentRecord other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
