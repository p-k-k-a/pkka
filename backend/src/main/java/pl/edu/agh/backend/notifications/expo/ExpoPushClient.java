package pl.edu.agh.backend.notifications.expo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class ExpoPushClient {

    private static final int MAX_MESSAGES_PER_REQUEST = 100;
    private static final int MAX_RECEIPTS_PER_REQUEST = 1000;

    private final RestClient restClient;

    public ExpoPushClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public SendOutcome send(List<ExpoPushMessage> messages) {
        Map<String, String> tickets = new HashMap<>();
        boolean delivered = true;
        for (List<ExpoPushMessage> chunk : chunks(messages, MAX_MESSAGES_PER_REQUEST)) {
            SendOutcome outcome = sendChunk(chunk);
            delivered &= outcome.delivered();
            tickets.putAll(outcome.tickets());
        }
        return new SendOutcome(delivered, tickets);
    }

    public Receipts fetchReceipts(List<String> ticketIds) {
        Set<String> checked = new HashSet<>();
        Set<String> deviceGone = new HashSet<>();
        for (List<String> chunk : chunks(ticketIds, MAX_RECEIPTS_PER_REQUEST)) {
            ExpoReceiptsResponse response = post("/getReceipts", Map.of("ids", chunk), ExpoReceiptsResponse.class);
            if (response == null || response.data() == null) {
                continue;
            }
            response.data().forEach((id, receipt) -> {
                checked.add(id);
                if (receipt.deviceGone()) {
                    deviceGone.add(id);
                }
            });
        }
        return new Receipts(checked, deviceGone);
    }

    private SendOutcome sendChunk(List<ExpoPushMessage> chunk) {
        ExpoPushResponse response = post("/send", chunk, ExpoPushResponse.class);
        if (response == null || response.data() == null) {
            return SendOutcome.refused();
        }
        Map<String, String> tickets = new HashMap<>();
        for (int i = 0; i < Math.min(response.data().size(), chunk.size()); i++) {
            ExpoPushTicket ticket = response.data().get(i);
            if (!ticket.ok()) {
                log.warn("Expo could not deliver a notification: {}", ticket.message());
            } else if (ticket.id() != null) {
                tickets.put(ticket.id(), chunk.get(i).to());
            }
        }
        return new SendOutcome(true, tickets);
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            return restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException ex) {
            log.error("Expo request to {} failed", path, ex);
            return null;
        }
    }

    private static <T> List<List<T>> chunks(List<T> items, int size) {
        return IntStream.range(0, (items.size() + size - 1) / size)
                .mapToObj(i -> items.subList(i * size, Math.min(items.size(), (i + 1) * size)))
                .toList();
    }
}
