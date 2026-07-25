package pl.edu.agh.backend.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.event.Tag;

@UtilityClass
public class UserSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /** Matches users who have at least one of the given tags (OR semantics), mirroring EventSpecifications#hasAnyTag. */
    public Specification<User> hasAnyTagId(Collection<UUID> tagIds) {
        return (root, query, cb) -> {
            if (tagIds == null || tagIds.isEmpty()) {
                return null;
            }
            query.distinct(true);
            Join<User, Tag> tags = root.join("tags");
            return tags.get("id").in(tagIds);
        };
    }

    /**
     * Free-text match against the profile fields available locally (position, company) and assigned tag names.
     * A left join is used for tags so that users without any tags can still match on position/company.
     */
    public Specification<User> matchesQuery(String rawQuery) {
        return (root, query, cb) -> {
            if (rawQuery == null || rawQuery.isBlank()) {
                return null;
            }
            query.distinct(true);
            String pattern = likePattern(rawQuery.trim());
            Join<User, Tag> tags = root.join("tags", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("currentPosition")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(root.get("company")), pattern, LIKE_ESCAPE),
                    cb.like(cb.lower(tags.get("name")), pattern, LIKE_ESCAPE));
        };
    }

    /** Escapes SQL LIKE wildcards ({@code %}, {@code _}) so user input is matched literally, not as a pattern. */
    private String likePattern(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
