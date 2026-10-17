package pl.edu.agh.backend.survey;

import java.util.ArrayList;
import java.util.List;
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

    private static void addPart(List<String> parts, String part) {
        String trimmed = part.trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }
}
