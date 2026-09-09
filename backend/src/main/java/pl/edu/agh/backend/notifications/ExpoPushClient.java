package pl.edu.agh.backend.notifications;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class ExpoPushClient {

    /** Expo counts recipients, not message objects, against this limit. */
    private static final int MAX_RECIPIENTS_PER_REQUEST = 100;

    private final RestClient restClient;

    @Autowired
    public ExpoPushClient(NotificationProperties properties) {
        this(RestClient.builder(), properties);
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

    /** Returns the tokens Expo reports as permanently unreachable, for the caller to delete. */
    public List<String> send(List<ExpoPushMessage> messages) {
        List<String> unreachable = new ArrayList<>();
        for (int from = 0; from < messages.size(); from += MAX_RECIPIENTS_PER_REQUEST) {
            int to = Math.min(messages.size(), from + MAX_RECIPIENTS_PER_REQUEST);
            unreachable.addAll(sendChunk(messages.subList(from, to)));
        }
        return unreachable;
    }

    /** A failed send is logged, never rethrown: a push nobody receives must not fail the request that caused it. */
    private List<String> sendChunk(List<ExpoPushMessage> chunk) {
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
            return List.of();
        }

        if (response == null || response.data() == null) {
            return List.of();
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
        return unreachable;
    }
}
