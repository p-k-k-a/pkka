package pl.edu.agh.backend.notifications.expo;

import java.util.Map;

public record SendOutcome(boolean delivered, Map<String, String> tickets) {

    static SendOutcome refused() {
        return new SendOutcome(false, Map.of());
    }
}
