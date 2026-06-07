package pl.edu.agh.backend.user;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Local application user record. Keycloak is the source of truth for identity and roles.
 * TODO: Profile fields (position, company, LinkedIn, GitHub, skills, etc.) will be added in a separate issue.
 */
@Entity
@Table(name = "users", indexes = @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class User {

    private static final ZoneId POLAND_ZONE = ZoneId.of("Europe/Warsaw");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Claim {@code sub} from the Keycloak token — user UUID in the pkka realm.
     */
    @Column(name = "keycloak_id", nullable = false, unique = true, length = 36)
    private String keycloakId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now(POLAND_ZONE);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(POLAND_ZONE);
    }
}
