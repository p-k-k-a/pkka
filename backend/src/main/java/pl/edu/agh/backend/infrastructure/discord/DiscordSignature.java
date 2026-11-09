package pl.edu.agh.backend.infrastructure.discord;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class DiscordSignature {

    // Turns the raw 32-byte key the Developer Portal shows into the X.509 encoding the JDK reads.
    private static final String X509_ED25519_PREFIX = "302a300506032b6570032100";

    private final PublicKey publicKey;

    DiscordSignature(DiscordProperties discord) {
        this.publicKey = parse(discord.publicKey());
    }

    boolean verify(String timestamp, byte[] body, String signatureHex) {
        if (publicKey == null || timestamp == null || signatureHex == null) {
            return false;
        }
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(timestamp.getBytes(UTF_8));
            signature.update(body);
            return signature.verify(HexFormat.of().parseHex(signatureHex));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static PublicKey parse(String publicKeyHex) {
        if (publicKeyHex == null || publicKeyHex.isBlank()) {
            return null;
        }
        try {
            byte[] x509 = HexFormat.of().parseHex(X509_ED25519_PREFIX + publicKeyHex.strip());
            return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(x509));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("discord.public-key is not an Ed25519 key in hex", ex);
        }
    }
}
