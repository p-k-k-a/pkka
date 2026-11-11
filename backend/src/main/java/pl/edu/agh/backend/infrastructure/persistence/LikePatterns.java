package pl.edu.agh.backend.infrastructure.persistence;

import java.util.Locale;
import lombok.experimental.UtilityClass;

/** Case-insensitive "contains" patterns for {@code cb.like(cb.lower(…), pattern, ESCAPE)}. */
@UtilityClass
public class LikePatterns {

    public static final char ESCAPE = '\\';

    /** Escapes {@code %} and {@code _}, so user input is matched literally. */
    public String contains(String raw) {
        String escaped = raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
