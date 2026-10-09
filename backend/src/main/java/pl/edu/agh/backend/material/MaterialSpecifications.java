package pl.edu.agh.backend.material;

import jakarta.persistence.criteria.Path;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.EventVisibility;
import pl.edu.agh.backend.security.Caller;

@UtilityClass
public class MaterialSpecifications {

    public Specification<Material> hasType(MaterialType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public Specification<Material> hasEventId(UUID eventId) {
        return (root, query, cb) ->
                eventId == null ? null : cb.equal(root.get("event").get("id"), eventId);
    }

    /**
     * Materials without a linked event are always visible. Materials linked to an event are only
     * visible if that event's audience is visible to the caller (mirrors {@link
     * EventVisibility#isVisibleTo}), so alumni-only or group-restricted events don't leak their
     * materials to callers who couldn't see the event itself.
     *
     * <p>Filters on {@code Material.eventAudience} rather than a join to {@code event}: the join
     * would apply {@code Event}'s soft-delete restriction, so a deleted event's materials would
     * come back as unlinked and visible to everyone.
     */
    public Specification<Material> visibleTo(Caller caller) {
        List<String> visibleAudiences =
                EventVisibility.audiencesOf(caller).stream().map(Audience::name).toList();
        return (root, query, cb) -> {
            Path<String> eventAudience = root.get("eventAudience");
            return cb.or(cb.isNull(eventAudience), eventAudience.in(visibleAudiences));
        };
    }
}
