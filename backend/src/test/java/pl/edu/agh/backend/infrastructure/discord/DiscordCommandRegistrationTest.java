package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordCommandRegistrationTest {

    private static final String APPLICATION_ID = "666666666666666666";
    private static final String BOT_TOKEN = "bot-token";
    private static final String COMMANDS_URI =
            "https://discord.com/api/v10/applications/" + APPLICATION_ID + "/commands";

    private MockRestServiceServer server;

    @Test
    void theCommandIsRegisteredForAdministratorsOnServersOnly() {
        DiscordCommandRegistration registration = registrationFor(BOT_TOKEN, APPLICATION_ID);
        server.expect(requestTo(COMMANDS_URI))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("konfiguracja"))
                .andExpect(jsonPath("$[0].default_member_permissions").value("8"))
                .andExpect(jsonPath("$[0].contexts[0]").value(0))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        registration.register();

        server.verify();
    }

    @Test
    void withoutAnApplicationIdNothingIsRegistered() {
        DiscordCommandRegistration registration = registrationFor(BOT_TOKEN, "");

        registration.register();

        server.verify();
    }

    @Test
    void withoutABotTokenNothingIsRegistered() {
        DiscordCommandRegistration registration = registrationFor("", APPLICATION_ID);

        registration.register();

        server.verify();
    }

    @Test
    void aRegistrationDiscordRefusesIsNotThrown() {
        DiscordCommandRegistration registration = registrationFor(BOT_TOKEN, APPLICATION_ID);
        server.expect(requestTo(COMMANDS_URI)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatCode(registration::register).doesNotThrowAnyException();
    }

    private DiscordCommandRegistration registrationFor(String botToken, String applicationId) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new DiscordCommandRegistration(builder.build(), new DiscordProperties(botToken, applicationId, null));
    }
}
