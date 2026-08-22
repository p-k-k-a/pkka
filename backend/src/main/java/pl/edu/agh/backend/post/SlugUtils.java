package pl.edu.agh.backend.post;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

final class SlugUtils {

    private SlugUtils() {}

    static String toSlug(String input) {
        return StringUtils.stripAccents(input)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }
}
