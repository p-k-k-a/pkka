package pl.edu.agh.backend.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

@UtilityClass
public class UserSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    /** Matches users who have at least one of the given skill tags (OR semantics), mirroring EventSpecifications#hasAnyTag. */
    public Specification<User> hasAnyTagId(Collection<UUID> tagIds) {
        return (root, query, cb) -> {
            if (tagIds == null || tagIds.isEmpty()) {
                return null;
            }
            query.distinct(true);
            Join<User, UserTag> tags = root.join("tags");
            return tags.get("id").in(tagIds);
        };
    }

    /** Matches users whose {@code willingToMentor} flag equals the given value; {@code null} means "don't filter". */
    public Specification<User> isMentor(Boolean mentor) {
        return (root, query, cb) -> {
            if (mentor == null) {
                return null;
            }
            return cb.equal(root.get("willingToMentor"), mentor);
        };
    }

    /**
     * Free-text match against the profile fields available locally: first/last name (only when the owner has not
     * hidden their name), position, company, and assigned skill tag names. A left join is used for tags so that
     * users without any tags can still match on the other fields.
     */
    public Specification<User> matchesQuery(String rawQuery) {
        return (root, query, cb) -> {
            if (rawQuery == null || rawQuery.isBlank()) {
                return null;
            }
            query.distinct(true);
            String pattern = likePattern(rawQuery.trim());
            Join<User, UserTag> tags = root.join("tags", JoinType.LEFT);

            Predicate nameMatch = cb.and(
                    cb.isTrue(root.get("showName")),
                    cb.or(
                            cb.like(cb.lower(root.get("firstName")), pattern, LIKE_ESCAPE),
                            cb.like(cb.lower(root.get("lastName")), pattern, LIKE_ESCAPE)));

            return cb.or(
                    nameMatch,
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
