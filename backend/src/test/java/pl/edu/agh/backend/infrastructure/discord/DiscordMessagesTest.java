package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jayway.jsonpath.JsonPath;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

class DiscordMessagesTest {

    private static final String CHANNEL_ID = "444444444444444444";
    private static final String MESSAGE_ID = "555555555555555555";
    private static final String BOT_TOKEN = "bot-token";

    private static final String CHANNEL_MESSAGES_URI =
            "https://discord.com/api/v10/channels/" + CHANNEL_ID + "/messages";
    private static final String MESSAGE_URI = CHANNEL_MESSAGES_URI + "/" + MESSAGE_ID;

    private static final DiscordEmbed EMBED = new DiscordEmbed("Warsztaty", "Opis", List.of());

    private MockRestServiceServer server;
    private DiscordMessages messages;

    @BeforeEach
    void setUp() {
        messages = messagesFor(BOT_TOKEN);
    }

    @Test
    void aPostedMessageIsKnownByTheIdDiscordGaveIt() {
        server.expect(requestTo(CHANNEL_MESSAGES_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andExpect(jsonPath("$.embeds[0].title").value("Warsztaty"))
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(MESSAGE_ID), MediaType.APPLICATION_JSON));

        assertThat(messages.post(CHANNEL_ID, EMBED)).contains(MESSAGE_ID);
        server.verify();
    }

    @Test
    void aPostCarriesANonceDiscordHoldsItTo() {
        server.expect(requestTo(CHANNEL_MESSAGES_URI))
                .andExpect(jsonPath("$.nonce").isString())
                .andExpect(jsonPath("$.enforce_nonce").value(true))
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(MESSAGE_ID), MediaType.APPLICATION_JSON));

        messages.post(CHANNEL_ID, EMBED);

        server.verify();
    }

    @Test
    void aPostCutOffOnTheWayBackIsAskedForAgainUnderTheSameNonce() {
        List<String> nonces = new ArrayList<>();
        RequestMatcher recordNonce = request ->
                nonces.add(JsonPath.read(((MockClientHttpRequest) request).getBodyAsString(), "$.nonce"));
        server.expect(requestTo(CHANNEL_MESSAGES_URI))
                .andExpect(recordNonce)
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        server.expect(requestTo(CHANNEL_MESSAGES_URI))
                .andExpect(recordNonce)
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(MESSAGE_ID), MediaType.APPLICATION_JSON));

        assertThat(messages.post(CHANNEL_ID, EMBED)).contains(MESSAGE_ID);
        assertThat(nonces).hasSize(2);
        assertThat(nonces.get(0)).hasSizeLessThanOrEqualTo(25).isEqualTo(nonces.get(1));
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"FORBIDDEN", "TOO_MANY_REQUESTS", "INTERNAL_SERVER_ERROR"})
    void aMessageDiscordRefusesIsReportedUnsentRatherThanThrown(HttpStatus status) {
        server.expect(requestTo(CHANNEL_MESSAGES_URI)).andRespond(withStatus(status));

        assertThat(messages.post(CHANNEL_ID, EMBED)).isEmpty();
    }

    @Test
    void anEditReplacesTheEmbedOfTheMessageInPlace() {
        server.expect(requestTo(MESSAGE_URI))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andExpect(jsonPath("$.embeds[0].title").value("Warsztaty"))
                .andRespond(withSuccess("{\"id\": \"%s\"}".formatted(MESSAGE_ID), MediaType.APPLICATION_JSON));

        messages.edit(CHANNEL_ID, MESSAGE_ID, EMBED);

        server.verify();
    }

    @Test
    void anEditDiscordRefusesIsNotThrown() {
        server.expect(requestTo(MESSAGE_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatCode(() -> messages.edit(CHANNEL_ID, MESSAGE_ID, EMBED)).doesNotThrowAnyException();
    }

    @Test
    void aDeletedMessageIsGone() {
        server.expect(requestTo(MESSAGE_URI))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertThat(messages.delete(CHANNEL_ID, MESSAGE_ID)).isTrue();
        server.verify();
    }

    @Test
    void aMessageAlreadyDeletedByHandCountsAsGone() {
        server.expect(requestTo(MESSAGE_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(messages.delete(CHANNEL_ID, MESSAGE_ID)).isTrue();
    }

    @Test
    void aMessageDiscordFailedToDeleteIsNotGone() {
        server.expect(requestTo(MESSAGE_URI)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(messages.delete(CHANNEL_ID, MESSAGE_ID)).isFalse();
    }

    @Test
    void withoutABotTokenNothingIsSentToDiscordAtAll() {
        messages = messagesFor("");

        assertThat(messages.post(CHANNEL_ID, EMBED)).isEmpty();
        messages.edit(CHANNEL_ID, MESSAGE_ID, EMBED);
        assertThat(messages.delete(CHANNEL_ID, MESSAGE_ID)).isFalse();
        server.verify();
    }

    private DiscordMessages messagesFor(String botToken) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new DiscordMessages(builder.build(), new DiscordProperties(botToken, null, null));
    }
}
