package pl.edu.agh.backend.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private MailService mail;

    @BeforeEach
    void setUp() throws Exception {
        when(sender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        mail = new MailService(
                sender,
                new InternetAddress("PKKA - WI AGH <kontakt@pkka.me>"),
                MailConfig.templateEngine(),
                "https://pkka.me");
    }

    @Test
    void sendsAnHtmlAndATextPartFromTheSender() throws Exception {
        boolean sent = mail.send("ala@example.com", "Witaj", "test-mail", Map.of("name", "Ala"));

        assertThat(sent).isTrue();
        MimeMessage message = captureSent();
        assertThat(message.getFrom()).containsExactly(new InternetAddress("PKKA - WI AGH <kontakt@pkka.me>"));
        assertThat(message.getAllRecipients()).containsExactly(new InternetAddress("ala@example.com"));
        assertThat(message.getSubject()).isEqualTo("Witaj");

        List<String> parts = textParts(message);
        assertThat(parts).hasSize(2);
        assertThat(parts.get(0))
                .contains("Cześć Ala!")
                .contains("Odpowiedz na tę wiadomość")
                .contains("https://pkka.me");
        assertThat(parts.get(1))
                .contains("Cześć <span>Ala</span>!")
                .contains("Klub Alumnów WI AGH")
                .contains("href=\"https://pkka.me\"");
    }

    @Test
    void escapesVariablesInTheHtmlPart() {
        String html = mail.render("test-mail.html", "Witaj", Map.of("name", "<b>Ala</b>"));

        assertThat(html).contains("&lt;b&gt;Ala&lt;/b&gt;").doesNotContain("<b>Ala</b>");
    }

    @Test
    void aFailedSendIsReportedInsteadOfThrown() {
        doThrow(new MailSendException("connection refused")).when(sender).send(any(MimeMessage.class));

        assertThat(mail.send("ala@example.com", "Witaj", "test-mail", Map.of("name", "Ala")))
                .isFalse();
    }

    private MimeMessage captureSent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }

    /** Plain text first, then HTML, as MimeMessageHelper lays out an alternative part. */
    private static List<String> textParts(Part part) throws Exception {
        List<String> texts = new ArrayList<>();
        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                texts.addAll(textParts(multipart.getBodyPart(i)));
            }
        } else if (content instanceof String text) {
            texts.add(text);
        }
        return texts;
    }
}
