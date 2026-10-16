package pl.edu.agh.backend.infrastructure.discord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DiscordGuildMembershipTest {

    private static final String GUILD_ID = "111111111111111111";
    private static final String DISCORD_ID = "999999999999999999";
    private static final String ALUMN_ROLE = "222222222222222222";
    private static final String OTHER_ROLE = "333333333333333333";
    private static final String BOT_TOKEN = "bot-token";

    private static final String MEMBER_URI =
            "https://discord.com/api/v10/guilds/" + GUILD_ID + "/members/" + DISCORD_ID;

    private MockRestServiceServer server;
    private DiscordGuildMembership membership;

    @BeforeEach
    void setUp() {
        membership = membershipFor(new DiscordProperties(GUILD_ID, BOT_TOKEN, List.of(ALUMN_ROLE)));
    }

    @Test
    void memberHoldingAnAcceptedRoleIsVouchedFor() {
        expectMember(HttpStatus.OK, """
                {"roles": ["%s", "%s"]}""".formatted(OTHER_ROLE, ALUMN_ROLE));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isTrue();
        server.verify();
    }

    @Test
    void memberWithoutAnyAcceptedRoleIsNotVouchedFor() {
        expectMember(HttpStatus.OK, """
                {"roles": ["%s"]}""".formatted(OTHER_ROLE));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
    }

    @Test
    void somebodyOutsideTheGuildIsNotVouchedFor() {
        expectMember(HttpStatus.NOT_FOUND, """
                {"message": "Unknown Member", "code": 10007}""");

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
    }

    @Test
    void rateLimitedDiscordIsNotVouchedForRatherThanFailingTheSubmission() {
        server.expect(requestTo(MEMBER_URI)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
    }

    @Test
    void unreachableDiscordIsNotVouchedForRatherThanFailingTheSubmission() {
        server.expect(requestTo(MEMBER_URI)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
    }

    @Test
    void noAcceptedRolesConfiguredVouchesForNobody() {
        membership = membershipFor(new DiscordProperties(GUILD_ID, BOT_TOKEN, null));
        expectMember(HttpStatus.OK, """
                {"roles": ["%s"]}""".formatted(ALUMN_ROLE));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
    }

    @Test
    void withoutABotTokenNothingIsAskedOfDiscordAtAll() {
        membership = membershipFor(new DiscordProperties(GUILD_ID, "", List.of(ALUMN_ROLE)));

        assertThat(membership.holdsAcceptedRole(DISCORD_ID)).isFalse();
        server.verify();
    }

    @Test
    void theBotTokenIsWhatAuthorisesTheLookup() {
        server.expect(requestTo(MEMBER_URI))
                .andExpect(header("Authorization", "Bot " + BOT_TOKEN))
                .andRespond(withSuccess("{\"roles\": []}", MediaType.APPLICATION_JSON));

        membership.holdsAcceptedRole(DISCORD_ID);

        server.verify();
    }

    private DiscordGuildMembership membershipFor(DiscordProperties properties) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        return new DiscordGuildMembership(builder.build(), properties);
    }

    private void expectMember(HttpStatus status, String body) {
        server.expect(requestTo(MEMBER_URI))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body));
    }
}
