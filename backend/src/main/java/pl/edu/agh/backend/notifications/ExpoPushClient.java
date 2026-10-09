package pl.edu.agh.backend.notifications;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends notifications through Expo's push service, which fans them out to FCM and APNs.
 *
 * <p>Delivery is best effort: a failed call is logged, never thrown, because whatever triggered the
 * notification has already been committed. Disabled by default so dev machines and tests don't call
 * Expo; set {@code app.notifications.expo.enabled=true} to send for real.
 */
@Component
@Slf4j
public class ExpoPushClient {

    /** Expo rejects requests with more messages than this. */
    static final int MAX_MESSAGES_PER_REQUEST = 100;

    private final RestClient restClient;
    private final boolean enabled;

    @Autowired
    public ExpoPushClient(
            @Value("${app.notifications.expo.enabled:false}") boolean enabled,
            @Value("${app.notifications.expo.url:https://exp.host/--/api/v2/push/send}") String url,
            @Value("${app.notifications.expo.access-token:}") String accessToken) {
        this(restClient(RestClient.builder().requestFactory(requestFactoryWithTimeouts()), url, accessToken), enabled);
    }

    ExpoPushClient(RestClient restClient, boolean enabled) {
        this.restClient = restClient;
        this.enabled = enabled;
    }

    private static JdkClientHttpRequestFactory requestFactoryWithTimeouts() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return requestFactory;
    }

    static RestClient restClient(RestClient.Builder builder, String url, String accessToken) {
        builder.baseUrl(url).defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(accessToken)) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return builder.build();
    }

    public void send(List<PushMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }
        if (!enabled) {
            log.info("Expo push disabled, not sending {} notification(s)", messages.size());
            return;
        }
        for (int from = 0; from < messages.size(); from += MAX_MESSAGES_PER_REQUEST) {
            List<PushMessage> chunk =
                    messages.subList(from, Math.min(from + MAX_MESSAGES_PER_REQUEST, messages.size()));
            try {
                restClient
                        .post()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(chunk.stream().map(ExpoPushClient::toExpo).toList())
                        .retrieve()
                        .toBodilessEntity();
            } catch (RestClientException ex) {
                log.warn("Sending {} push notification(s) through Expo failed", chunk.size(), ex);
            }
        }
    }

    private static Map<String, Object> toExpo(PushMessage message) {
        return Map.of(
                "to", message.to(),
                "title", message.title(),
                "body", message.body(),
                "data", message.data(),
                "sound", "default");
    }
}
