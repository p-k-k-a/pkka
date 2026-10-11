package pl.edu.agh.backend.infrastructure.validation;

import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TagNames {

    public static final String PATTERN = "^(?=.*[\\p{L}\\p{N}])[\\p{L}\\p{N}+#./ -]{1,32}$";

    public String normalize(String raw) {
        return raw == null ? null : raw.strip().replaceAll("\\s+", " ");
    }

    public String key(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }
}
