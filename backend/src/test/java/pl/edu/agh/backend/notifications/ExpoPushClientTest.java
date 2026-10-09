package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
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

    private static final String URL = "https://expo.test/push/send";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    void postsMessagesInExposShape() {
        ExpoPushClient client = new ExpoPushClient(ExpoPushClient.restClient(builder, URL, "secret"), true);
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[a]"))
                .andExpect(jsonPath("$[0].title").value("Tytuł"))
                .andExpect(jsonPath("$[0].data.eventId").value("42"))
                .andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));

        client.send(List.of(message("ExponentPushToken[a]")));

        server.verify();
    }

    @Test
    void splitsIntoRequestsExpoAccepts() {
        ExpoPushClient client = new ExpoPushClient(ExpoPushClient.restClient(builder, URL, ""), true);
        server.expect(times(2), requestTo(URL)).andRespond(withSuccess());

        client.send(IntStream.range(0, ExpoPushClient.MAX_MESSAGES_PER_REQUEST + 1)
                .mapToObj(i -> message("ExponentPushToken[" + i + "]"))
                .toList());

        server.verify();
    }

    @Test
    void swallowsServerErrors() {
        ExpoPushClient client = new ExpoPushClient(ExpoPushClient.restClient(builder, URL, ""), true);
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatCode(() -> client.send(List.of(message("ExponentPushToken[a]"))))
                .doesNotThrowAnyException();
    }

    @Test
    void sendsNothingWhenDisabled() {
        ExpoPushClient client = new ExpoPushClient(ExpoPushClient.restClient(builder, URL, ""), false);

        client.send(List.of(message("ExponentPushToken[a]")));

        server.verify();
    }

    private static PushMessage message(String token) {
        return new PushMessage(token, "Tytuł", "Treść", Map.of("eventId", "42"));
    }
}
