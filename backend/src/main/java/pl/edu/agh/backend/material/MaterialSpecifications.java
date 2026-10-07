package pl.edu.agh.backend.material;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.Set;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventVisibility;
import pl.edu.agh.backend.infrastructure.persistence.LikePatterns;
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
     * Free-text match against the material's title and description and its linked event's title, so
     * searching for an event also finds its recordings. The event join is LEFT so materials without
     * an event can still match on their own fields.
     */
    public Specification<Material> matchesQuery(String rawQuery) {
        return (root, query, cb) -> {
            if (rawQuery == null || rawQuery.isBlank()) {
                return null;
            }
            String pattern = LikePatterns.contains(rawQuery.trim());
            Join<Material, Event> event = root.join("event", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, LikePatterns.ESCAPE),
                    cb.like(cb.lower(root.get("description")), pattern, LikePatterns.ESCAPE),
                    cb.like(cb.lower(event.get("title")), pattern, LikePatterns.ESCAPE));
        };
    }

    /**
     * Materials without a linked event are always visible. Materials linked to an event are only
     * visible if that event's audience is visible to the caller (mirrors {@link
     * EventVisibility#isVisibleTo}), so alumni-only or group-restricted events don't leak their
     * materials to callers who couldn't see the event itself.
     *
     * <p>The join to {@code event} must be explicit and LEFT: a plain {@code root.get("event")}
     * path expression lets Hibernate pick an INNER join, which would silently drop every
     * material that has no linked event at all before the OR-condition below is even evaluated.
     */
    public Specification<Material> visibleTo(Caller caller) {
        Set<Audience> visibleAudiences = EventVisibility.audiencesOf(caller);
        return (root, query, cb) -> {
            Join<Material, Event> event = root.join("event", JoinType.LEFT);
            return cb.or(cb.isNull(event.get("id")), event.get("audience").in(visibleAudiences));
        };
    }
}
