package pl.edu.agh.backend.notifications.expo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushClientTest {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";
    private static final String RECEIPTS_URL = "https://exp.host/--/api/v2/push/getReceipts";

    private MockRestServiceServer server;
    private ExpoPushClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExpoPushClient(ExpoRestClientConfig.configure(builder, "https://exp.host/--/api/v2/push", "")
                .build());
    }

    private static ExpoPushMessage message(String token) {
        return new ExpoPushMessage(token, "Nowe wydarzenie", "Warsztaty", "events", Map.of("type", "X"));
    }

    @Test
    void send_mapsTicketsInTheSecondChunkToTheRightTokens() {
        server.expect(requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess(okTickets(100, "first"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(20))
                .andRespond(withSuccess(okTickets(20, "second"), MediaType.APPLICATION_JSON));

        SendOutcome outcome = client.send(
                IntStream.range(0, 120).mapToObj(i -> message("token-" + i)).toList());

        assertThat(outcome.tickets()).hasSize(120).containsEntry("second-19", "token-119");
        server.verify();
    }

    private static String okTickets(int count, String prefix) {
        return """
                {"data":[%s]}""".formatted(IntStream.range(0, count)
                .mapToObj(i -> "{\"status\":\"ok\",\"id\":\"%s-%d\"}".formatted(prefix, i))
                .collect(Collectors.joining(",")));
    }

    @Test
    void send_swallowsATransportFailure() {
        server.expect(requestTo(SEND_URL)).andRespond(withServerError());

        SendOutcome outcome = client.send(List.of(message("a")));

        assertThat(outcome.delivered()).isFalse();
        assertThat(outcome.tickets()).isEmpty();
        server.verify();
    }

    @Test
    void send_remembersWhichTokenEachTicketBelongsTo() {
        server.expect(requestTo(SEND_URL)).andRespond(withSuccess("""
                        {"data":[
                          {"status":"ok","id":"ticket-a"},
                          {"status":"ok","id":"ticket-b"},
                          {"status":"error","message":"too big","details":{"error":"MessageTooBig"}}
                        ]}""", MediaType.APPLICATION_JSON));

        SendOutcome outcome = client.send(List.of(message("a"), message("b"), message("c")));

        assertThat(outcome.tickets()).containsExactlyInAnyOrderEntriesOf(Map.of("ticket-a", "a", "ticket-b", "b"));
        server.verify();
    }

    @Test
    void fetchReceipts_reportsGoneDevicesAndOnlyTheReceiptsExpoHasReady() {
        server.expect(requestTo(RECEIPTS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.ids.length()").value(3))
                .andRespond(withSuccess("""
                        {"data":{
                          "ticket-a":{"status":"ok"},
                          "ticket-b":{"status":"error","message":"gone","details":{"error":"DeviceNotRegistered"}}
                        }}""", MediaType.APPLICATION_JSON));

        Receipts receipts = client.fetchReceipts(List.of("ticket-a", "ticket-b", "ticket-c"));

        assertThat(receipts.checked()).containsExactlyInAnyOrder("ticket-a", "ticket-b");
        assertThat(receipts.deviceGone()).containsExactly("ticket-b");
        server.verify();
    }

    @Test
    void fetchReceipts_treatsAFailureAsNothingChecked() {
        server.expect(requestTo(RECEIPTS_URL)).andRespond(withServerError());

        Receipts receipts = client.fetchReceipts(List.of("ticket-a"));

        assertThat(receipts.checked()).isEmpty();
        assertThat(receipts.deviceGone()).isEmpty();
        server.verify();
    }
}
