package pl.edu.agh.backend.infrastructure.discord;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiscordEmbed(String title, String description, List<Field> fields) {

    private static final int TITLE_LIMIT = 256;
    private static final int DESCRIPTION_LIMIT = 4096;
    private static final int FIELD_NAME_LIMIT = 256;
    private static final int FIELD_VALUE_LIMIT = 1024;

    public DiscordEmbed {
        title = truncate(title, TITLE_LIMIT);
        description = truncate(description, DESCRIPTION_LIMIT);
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public record Field(String name, String value) {

        public Field {
            name = truncate(name, FIELD_NAME_LIMIT);
            value = truncate(value, FIELD_VALUE_LIMIT);
        }
    }

    private static String truncate(String text, int limit) {
        if (text == null || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit - 1) + "…";
    }
}
