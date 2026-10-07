package pl.edu.agh.backend.infrastructure.persistence;

import java.util.Locale;
import lombok.experimental.UtilityClass;

/** Case-insensitive "contains" matching for free-text search in JPA specifications. */
@UtilityClass
public class LikePatterns {

    /** The escape character to pass to {@code cb.like(…, pattern, ESCAPE)} alongside {@link #contains}. */
    public static final char ESCAPE = '\\';

    /**
     * {@code %raw%} in lower case, with the LIKE wildcards ({@code %}, {@code _}) escaped so user input is matched
     * literally, not as a pattern. Compare it against {@code cb.lower(…)}.
     */
    public String contains(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
