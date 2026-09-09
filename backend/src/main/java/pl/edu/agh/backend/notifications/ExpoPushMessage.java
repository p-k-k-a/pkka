package pl.edu.agh.backend.notifications;

import java.util.Map;

public record ExpoPushMessage(String to, String title, String body, String channelId, Map<String, String> data) {}
