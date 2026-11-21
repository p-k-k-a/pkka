package pl.edu.agh.backend.event;

import jakarta.persistence.criteria.Join;
import java.time.Instant;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.event.tag.Tag;

@UtilityClass
public class EventSpecifications {

    // An event stays upcoming until it ends, so one under way (e.g. an online meeting people are
    // still joining) is not archived the moment it starts.
    public Specification<Event> endsAfter(Instant dateTime) {
        return (root, query, cb) -> dateTime == null ? null : cb.greaterThan(root.get("endsAt"), dateTime);
    }

    public Specification<Event> endsBeforeOrEqual(Instant dateTime) {
        return (root, query, cb) -> dateTime == null ? null : cb.lessThanOrEqualTo(root.get("endsAt"), dateTime);
    }

    public Specification<Event> audienceIn(Collection<Audience> audiences) {
        return (root, query, cb) -> (audiences == null || audiences.isEmpty())
                ? null
                : root.get("audience").in(audiences);
    }

    public Specification<Event> hasAnyTag(Collection<String> tagNames) {
        return (root, query, cb) -> {
            if (tagNames == null || tagNames.isEmpty()) return null;
            query.distinct(true);
            Join<Event, Tag> tags = root.join("tags");
            return tags.get("name").in(tagNames);
        };
    }
}
