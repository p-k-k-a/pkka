package pl.edu.agh.backend.survey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
class SurveyAnswerValues {

    static List<String> commaSeparated(String value) {
        List<String> parts = new ArrayList<>();
        int from = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == ',') {
                addPart(parts, value.substring(from, i));
                from = i + 1;
            }
        }
        addPart(parts, value.substring(from));
        return parts;
    }

    /**
     * A grid answer as stored: {@code rowId:optionId} pairs joined with commas. Only for values the
     * service has already validated, so a malformed pair is a bug and fails loudly.
     */
    static Map<UUID, UUID> gridChoices(String value) {
        Map<UUID, UUID> choices = new LinkedHashMap<>();
        for (String pair : commaSeparated(value)) {
            int colon = pair.indexOf(':');
            choices.put(UUID.fromString(pair.substring(0, colon)), UUID.fromString(pair.substring(colon + 1)));
        }
        return choices;
    }

    private static void addPart(List<String> parts, String part) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }
}
