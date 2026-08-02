package pl.edu.agh.backend.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Local application user record. Keycloak is the source of truth for identity and roles.
 * Profile fields are stored here; identity/auth data stays in Keycloak.
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

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "discord_id", length = 32)
    private String discordId;

    @Column(name = "show_name", nullable = false)
    private boolean showName = true;

    @Column(name = "show_email", nullable = false)
    private boolean showEmail = true;

    @Column(name = "show_discord", nullable = false)
    private boolean showDiscord = true;

    @Column(name = "current_position", length = 255)
    private String currentPosition;

    @Column(name = "company", length = 255)
    private String company;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "willing_to_mentor", nullable = false)
    private boolean willingToMentor = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_tags",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @BatchSize(size = 30)
    private Set<UserTag> tags = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
