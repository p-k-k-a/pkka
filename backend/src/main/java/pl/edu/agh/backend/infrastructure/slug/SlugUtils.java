package pl.edu.agh.backend.infrastructure.slug;

import java.util.Locale;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

public final class SlugUtils {

    private static final int MAX_BASE_LENGTH = 250;

    private SlugUtils() {}

    public static String uniqueSlug(String title, Predicate<String> taken) {
        String base = toSlug(title);
        if (base.length() > MAX_BASE_LENGTH) {
            base = base.substring(0, MAX_BASE_LENGTH);
        }
        String candidate = base;
        int suffix = 2;
        while (taken.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    public static String toSlug(String input) {
        return StringUtils.stripAccents(input)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
    }
}
