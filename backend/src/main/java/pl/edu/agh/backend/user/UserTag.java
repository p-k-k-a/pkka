package pl.edu.agh.backend.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.*;

/**
 * Alumni skill tag (e.g. "Java", "DevOps"), assignable by a user to their own profile. Deliberately a
 * separate entity/table from {@link pl.edu.agh.backend.event.tag.Tag}: event tags (categories like
 * "workshop", "networking") and user skill tags are semantically disjoint domains that happened to
 * share a catalog table early on. Keeping them separate avoids a shared uniqueness constraint on
 * {@code name} across two unrelated concepts and lets each catalog evolve independently.
 */
@Entity
@Table(
        name = "user_skill_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_skill_tags_name", columnNames = "name"))
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class UserTag {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 32)
    @Column(name = "name", nullable = false, length = 32)
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserTag other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
