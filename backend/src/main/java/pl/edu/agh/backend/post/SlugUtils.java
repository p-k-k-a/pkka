package pl.edu.agh.backend.post;

import java.text.Normalizer;
import java.util.Locale;

final class SlugUtils {

    private SlugUtils() {}

    /**
     * Generates a slug from any text.
     * "Welcome to AGH Alumni!" -> "welcome-to-agh-alumni"
     */
    static String toSlug(String input) {
        // NFD does not decompose the stroked "ł", so it must be mapped by hand
        // or titles like "Koło naukowe" would collapse to "koo-naukowe".
        return Normalizer.normalize(input.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }
}
