package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushClientTest {

    private static final String SEND_URL = "https://exp.host/--/api/v2/push/send";

    private MockRestServiceServer server;
    private ExpoPushClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ExpoPushClient(builder, new NotificationProperties(true, "https://exp.host/--/api/v2/push", ""));
    }

    private static ExpoPushMessage message(String token) {
        return new ExpoPushMessage(token, "Nowe wydarzenie", "Warsztaty", "events", Map.of("type", "X"));
    }

    @Test
    void send_reportsOnlyTheTokensExpoSaysAreGone() {
        server.expect(requestTo(SEND_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"data":[
                          {"status":"ok","id":"1"},
                          {"status":"error","message":"gone","details":{"error":"DeviceNotRegistered"}},
                          {"status":"error","message":"too big","details":{"error":"MessageTooBig"}}
                        ]}""", MediaType.APPLICATION_JSON));

        List<String> unreachable = client.send(List.of(message("a"), message("b"), message("c")));

        assertThat(unreachable).containsExactly("b");
        server.verify();
    }

    @Test
    void send_splitsAtOneHundredRecipients() {
        server.expect(requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess("""
                        {"data":[]}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo(SEND_URL))
                .andExpect(jsonPath("$.length()").value(50))
                .andRespond(withSuccess("""
                        {"data":[]}""", MediaType.APPLICATION_JSON));

        client.send(IntStream.range(0, 150).mapToObj(i -> message("token-" + i)).toList());

        server.verify();
    }

    @Test
    void send_swallowsATransportFailure() {
        server.expect(requestTo(SEND_URL)).andRespond(withServerError());

        assertThat(client.send(List.of(message("a")))).isEmpty();
        server.verify();
    }
}
