package pl.edu.agh.backend.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MailConfigTest {

    private final MailConfig config = new MailConfig();

    @Test
    void mailIsDisabledWithoutAHost() {
        MailService mail = config.mailService("", 587, "", "", true, "", "https://pkka.me");

        assertThat(mail.isEnabled()).isFalse();
        assertThat(mail.send("ala@example.com", "Temat", "test-mail", Map.of("name", "Ala")))
                .isFalse();
    }

    @Test
    void mailpitNeedsNeitherCredentialsNorTls() {
        MailService mail = config.mailService(
                "localhost", 1025, "", "", false, "PKKA - WI AGH <kontakt@pkka.me>", "http://localhost:3000");

        assertThat(mail.isEnabled()).isTrue();
    }

    @Test
    void failsStartupWhenOnlyTheUsernameIsSet() {
        assertThatThrownBy(() -> config.mailService(
                        "smtp-relay.brevo.com", 587, "login", "", true, "kontakt@pkka.me", "https://pkka.me"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_PASSWORD");
    }

    @Test
    void failsStartupWithoutASender() {
        assertThatThrownBy(() ->
                        config.mailService("smtp-relay.brevo.com", 587, "login", "key", true, "", "https://pkka.me"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_FROM");
    }

    @Test
    void failsStartupWithAMalformedSender() {
        assertThatThrownBy(() -> config.mailService(
                        "smtp-relay.brevo.com", 587, "login", "key", true, "kontakt at pkka.me", "https://pkka.me"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_FROM");
    }

    @Test
    void failsStartupWithoutTheWebAddressTheMailsLinkTo() {
        assertThatThrownBy(() ->
                        config.mailService("smtp-relay.brevo.com", 587, "login", "key", true, "kontakt@pkka.me", " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WEB_BASE_URL");
    }

    @Test
    void dropsTheTrailingSlashOfTheWebAddress() {
        MailService mail = config.mailService("localhost", 1025, "", "", false, "kontakt@pkka.me", "https://pkka.me/");

        assertThat(mail.render("test-mail.txt", "Temat", Map.of("name", "Ala")))
                .contains("https://pkka.me")
                .doesNotContain("https://pkka.me/");
    }
}
