package pl.edu.agh.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Local application user record. Keycloak is the source of truth for identity and roles.
 * TODO: Profile fields (position, company, LinkedIn, GitHub, skills, etc.) will be added in a separate issue.
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id", unique = true))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Claim {@code sub} from the Keycloak token — user UUID in the pkka realm.
     */
    @Column(name = "keycloak_id", nullable = false, unique = true, length = 36)
    private String keycloakId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
