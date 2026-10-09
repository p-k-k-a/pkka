package pl.edu.agh.backend.notifications;

import java.util.Map;

/** One notification for one device, in the shape Expo's push API takes it. */
public record PushMessage(String to, String title, String body, Map<String, String> data) {

    public PushMessage {
        data = Map.copyOf(data);
    }
}
