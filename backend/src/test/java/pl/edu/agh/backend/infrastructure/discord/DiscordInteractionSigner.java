package pl.edu.agh.backend.infrastructure.discord;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Arrays;
import java.util.HexFormat;

final class DiscordInteractionSigner {

    private static final int RAW_KEY_LENGTH = 32;

    private final KeyPair keys;

    DiscordInteractionSigner() {
        try {
            keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    String publicKeyHex() {
        byte[] x509 = keys.getPublic().getEncoded();
        return HexFormat.of().formatHex(Arrays.copyOfRange(x509, x509.length - RAW_KEY_LENGTH, x509.length));
    }

    String sign(String timestamp, byte[] body) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initSign(keys.getPrivate());
            signature.update(timestamp.getBytes(UTF_8));
            signature.update(body);
            return HexFormat.of().formatHex(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
