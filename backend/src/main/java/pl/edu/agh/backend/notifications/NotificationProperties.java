package pl.edu.agh.backend.notifications;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        @DefaultValue("https://exp.host/--/api/v2/push") String baseUrl,
        @DefaultValue("") String accessToken) {}
