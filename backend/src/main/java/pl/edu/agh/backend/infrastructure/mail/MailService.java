package pl.edu.agh.backend.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Sends one mail rendered from {@code templates/mail/<template>.html} and {@code .txt}. Built by
 * {@link MailConfig}.
 *
 * <p>Never throws: a mail is a side effect of something that already happened (a promotion, an event change),
 * so a failed send is logged and reported as {@code false}, not rolled back into the caller. Callers send
 * after commit and off the request thread ({@code @Async} listeners).
 */
@Slf4j
public class MailService {

    private static final Locale POLISH = Locale.forLanguageTag("pl-PL");

    /** {@code null} when mail is not configured. */
    private final JavaMailSender sender;

    private final InternetAddress from;
    private final TemplateEngine templates;
    private final String webBaseUrl;

    MailService(JavaMailSender sender, InternetAddress from, TemplateEngine templates, String webBaseUrl) {
        this.sender = sender;
        this.from = from;
        this.templates = templates;
        this.webBaseUrl = webBaseUrl;
    }

    public boolean isEnabled() {
        return sender != null;
    }

    /**
     * Returns whether the mail was handed to the SMTP server. {@code variables} are available in both templates,
     * next to {@code subject} and {@code webBaseUrl}.
     */
    public boolean send(String to, String subject, String template, Map<String, ?> variables) {
        if (sender == null) {
            log.info("Mail is not configured, skipping '{}' mail", template);
            return false;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(
                    render(template + ".txt", subject, variables), render(template + ".html", subject, variables));
            sender.send(message);
            return true;
        } catch (MailException | MessagingException ex) {
            log.warn("Could not send '{}' mail: {}", template, ex.getMessage());
            return false;
        }
    }

    String render(String templateName, String subject, Map<String, ?> variables) {
        Map<String, Object> all = new HashMap<>(variables);
        all.put("subject", subject);
        all.put("webBaseUrl", webBaseUrl);
        return templates.process(templateName, new Context(POLISH, all));
    }
}
