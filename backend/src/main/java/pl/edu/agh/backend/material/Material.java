package pl.edu.agh.backend.material;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.edu.agh.backend.event.Event;

@Entity
@Table(name = "materials")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 300)
    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MaterialType type;

    @NotBlank
    @Size(max = 2000)
    @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
    @Column(nullable = false, length = 2000)
    private String url;

    /**
     * Optional linked event. The referenced event may be soft-deleted after this material was
     * created; {@link NotFoundAction#IGNORE} makes Hibernate return {@code null} instead of
     * throwing {@link jakarta.persistence.EntityNotFoundException} in that case, since
     * {@code Event} filters soft-deleted rows out of every query via {@code @SQLRestriction}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @NotFound(action = NotFoundAction.IGNORE)
    private Event event;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
