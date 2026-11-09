package pl.edu.agh.backend.infrastructure.discord;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DiscordSignatureTest {

    private static final String TIMESTAMP = "1760000000";
    private static final byte[] BODY = "{\"type\":1}".getBytes(UTF_8);

    private final DiscordInteractionSigner discord = new DiscordInteractionSigner();
    private final DiscordSignature signature = signatureTrusting(discord.publicKeyHex());

    @Test
    void anInteractionDiscordSignedIsTrusted() {
        assertThat(signature.verify(TIMESTAMP, BODY, discord.sign(TIMESTAMP, BODY)))
                .isTrue();
    }

    @Test
    void aBodyChangedAfterSigningIsNotTrusted() {
        byte[] changed = "{\"type\":2}".getBytes(UTF_8);

        assertThat(signature.verify(TIMESTAMP, changed, discord.sign(TIMESTAMP, BODY)))
                .isFalse();
    }

    @Test
    void aSignatureMovedToAnotherTimestampIsNotTrusted() {
        assertThat(signature.verify("1760000001", BODY, discord.sign(TIMESTAMP, BODY)))
                .isFalse();
    }

    @Test
    void aSignatureByAnybodyButDiscordIsNotTrusted() {
        DiscordInteractionSigner impostor = new DiscordInteractionSigner();

        assertThat(signature.verify(TIMESTAMP, BODY, impostor.sign(TIMESTAMP, BODY)))
                .isFalse();
    }

    @Test
    void aMissingOrMangledSignatureIsNotTrustedRatherThanThrown() {
        assertThat(signature.verify(TIMESTAMP, BODY, null)).isFalse();
        assertThat(signature.verify(null, BODY, discord.sign(TIMESTAMP, BODY))).isFalse();
        assertThat(signature.verify(TIMESTAMP, BODY, "not hex")).isFalse();
        assertThat(signature.verify(TIMESTAMP, BODY, "abcd")).isFalse();
    }

    @Test
    void withoutAPublicKeyNothingIsTrusted() {
        DiscordSignature unconfigured = signatureTrusting("");

        assertThat(unconfigured.verify(TIMESTAMP, BODY, discord.sign(TIMESTAMP, BODY)))
                .isFalse();
    }

    @Test
    void aMalformedPublicKeyStopsTheApplicationStarting() {
        assertThatThrownBy(() -> signatureTrusting("abcd")).isInstanceOf(IllegalStateException.class);
    }

    private static DiscordSignature signatureTrusting(String publicKeyHex) {
        return new DiscordSignature(new DiscordProperties(null, null, publicKeyHex));
    }
}
