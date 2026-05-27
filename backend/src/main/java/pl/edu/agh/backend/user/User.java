package pl.edu.agh.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Claim {@code sub} from the Keycloak token — user UUID in the pkka realm.
     */
    @Column(name = "keycloak_id", nullable = false, unique = true, length = 36)
    private String keycloakId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
