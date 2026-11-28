package pl.edu.agh.backend.topicgroup;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.edu.agh.backend.user.User;

/**
 * A thematic group ("grupa tematyczna") alumni gather in. Groups come from an admin approving a
 * {@link TopicGroupRequest}, and the {@link #coordinator} runs the group from then on.
 */
@Entity
@Table(name = "topic_groups")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class TopicGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @ToString.Include
    private UUID id;

    /** Derived from the name, the way events and posts derive theirs; never changes, so links keep working. */
    @NotBlank
    @Size(max = 255)
    @Column(name = "slug", nullable = false, updatable = false, length = 255)
    @ToString.Include
    private String slug;

    @NotBlank
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    @ToString.Include
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** The group's own admin — the alumn who asked for it, not a club admin. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @Column(name = "discord_role_id", length = 32)
    private String discordRoleId;

    @Column(name = "discord_channel_id", length = 32)
    private String discordChannelId;

    /** Joins up to this moment were already reported to the coordinator in the daily digest. */
    @Column(name = "last_join_digest_at")
    private Instant lastJoinDigestAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public boolean isCoordinatedBy(UUID userId) {
        return coordinator != null && coordinator.getId().equals(userId);
    }

    /** Link to the group's Discord channel; null until the channel exists and the server is set up. */
    public String discordChannelUrl(Optional<String> guildId) {
        if (discordChannelId == null) {
            return null;
        }
        return guildId.map(id -> "https://discord.com/channels/%s/%s".formatted(id, discordChannelId))
                .orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicGroup other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
