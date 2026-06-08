package pl.edu.agh.backend.repository;

import jakarta.persistence.criteria.Join;
import java.time.Instant;
import java.util.Collection;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.domain.Audience;
import pl.edu.agh.backend.domain.Event;
import pl.edu.agh.backend.domain.Tag;

@UtilityClass
public class EventSpecifications {

    public Specification<Event> startsAfter(Instant dateTime) {
        return (root, query, cb) -> dateTime == null ? null : cb.greaterThan(root.get("startsAt"), dateTime);
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
