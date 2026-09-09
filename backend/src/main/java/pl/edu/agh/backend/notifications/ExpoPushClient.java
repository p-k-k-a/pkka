package pl.edu.agh.backend.notifications;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class ExpoPushClient {

    /** Expo counts recipients, not message objects, against this limit. */
    private static final int MAX_RECIPIENTS_PER_REQUEST = 100;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    /**
     * What one send attempt achieved: whether Expo accepted it at all, and which tokens it says are dead.
     * The caller needs both — a refused batch must be retried later, a dead token must be forgotten now.
     */
    public record Outcome(boolean delivered, List<String> unreachableTokens) {
        static Outcome refused() {
            return new Outcome(false, List.of());
        }
    }

    private final RestClient restClient;

    @Autowired
    public ExpoPushClient(NotificationProperties properties) {
        this(RestClient.builder().requestFactory(timeoutFactory()), properties);
    }

    /** Without these the JDK client waits forever, and the caller's thread and DB connection wait with it. */
    private static ClientHttpRequestFactory timeoutFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** Takes a builder so a test can bind a {@code MockRestServiceServer} to it; the app has no builder bean. */
    ExpoPushClient(RestClient.Builder builder, NotificationProperties properties) {
        RestClient.Builder configured = builder.baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        if (StringUtils.hasText(properties.accessToken())) {
            configured = configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken());
        }
        this.restClient = configured.build();
    }

    /** Returns whether every chunk was accepted, plus the tokens Expo reports as permanently unreachable. */
    public Outcome send(List<ExpoPushMessage> messages) {
        List<String> unreachable = new ArrayList<>();
        boolean delivered = true;
        for (int from = 0; from < messages.size(); from += MAX_RECIPIENTS_PER_REQUEST) {
            int to = Math.min(messages.size(), from + MAX_RECIPIENTS_PER_REQUEST);
            Outcome outcome = sendChunk(messages.subList(from, to));
            delivered &= outcome.delivered();
            unreachable.addAll(outcome.unreachableTokens());
        }
        return new Outcome(delivered, unreachable);
    }

    /** A failed send is logged, never rethrown: a push nobody receives must not fail the request that caused it. */
    private Outcome sendChunk(List<ExpoPushMessage> chunk) {
        ExpoPushResponse response;
        try {
            response = restClient
                    .post()
                    .uri("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(chunk)
                    .retrieve()
                    .body(ExpoPushResponse.class);
        } catch (RestClientException ex) {
            log.error("Expo rejected a batch of {} notifications", chunk.size(), ex);
            return Outcome.refused();
        }

        if (response == null || response.data() == null) {
            return Outcome.refused();
        }

        List<String> unreachable = new ArrayList<>();
        // Tickets come back positionally, so the token a ticket refers to is the one at the same index.
        for (int i = 0; i < Math.min(response.data().size(), chunk.size()); i++) {
            ExpoPushTicket ticket = response.data().get(i);
            if (ticket.deviceGone()) {
                unreachable.add(chunk.get(i).to());
            } else if (!ticket.ok()) {
                log.warn("Expo could not deliver a notification: {}", ticket.message());
            }
        }
        return new Outcome(true, unreachable);
    }
}
