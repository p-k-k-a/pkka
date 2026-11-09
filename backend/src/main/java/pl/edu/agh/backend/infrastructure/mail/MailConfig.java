package pl.edu.agh.backend.infrastructure.mail;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Outgoing mail over SMTP: Mailpit locally, Brevo in production. Configured with {@code app.mail.*}, fed from
 * the {@code MAIL_*} environment variables.
 *
 * <p>An empty {@code MAIL_HOST} disables mail (sending is skipped and logged). A username without a password,
 * or the other way round, is a deployment mistake and fails startup rather than surfacing later as a mail that
 * never arrived.
 */
@Slf4j
@Configuration
class MailConfig {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    @Bean
    MailService mailService(
            @Value("${app.mail.host:}") String host,
            @Value("${app.mail.port:587}") int port,
            @Value("${app.mail.username:}") String username,
            @Value("${app.mail.password:}") String password,
            @Value("${app.mail.starttls:true}") boolean starttls,
            @Value("${app.mail.from:}") String from,
            @Value("${app.web.base-url:}") String webBaseUrl) {
        TemplateEngine templates = templateEngine();
        if (host.isBlank()) {
            log.warn("Mail is not configured (MAIL_HOST is unset); mails will be skipped");
            return new MailService(null, null, templates, webBaseUrl);
        }
        if (username.isBlank() != password.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME and MAIL_PASSWORD must be set together");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.connectiontimeout", CONNECT_TIMEOUT_MS);
        properties.put("mail.smtp.timeout", READ_TIMEOUT_MS);
        properties.put("mail.smtp.writetimeout", READ_TIMEOUT_MS);
        if (!username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
            properties.put("mail.smtp.auth", "true");
        }
        if (starttls) {
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
        }
        return new MailService(sender, parseFrom(from), templates, webBaseUrl);
    }

    private static InternetAddress parseFrom(String from) {
        if (from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM must be set when MAIL_HOST is set");
        }
        try {
            InternetAddress address = new InternetAddress(from, true);
            address.validate();
            return address;
        } catch (AddressException ex) {
            throw new IllegalStateException("MAIL_FROM is not a valid address: " + from, ex);
        }
    }

    /**
     * Templates live in {@code templates/mail/} on the classpath. The extension picks the mode: {@code .html} is
     * the HTML part, {@code .txt} the plain-text part of the same mail.
     */
    static TemplateEngine templateEngine() {
        TemplateEngine engine = new TemplateEngine();
        engine.addTemplateResolver(resolver(TemplateMode.HTML, "*.html", 1));
        engine.addTemplateResolver(resolver(TemplateMode.TEXT, "*.txt", 2));
        return engine;
    }

    private static ClassLoaderTemplateResolver resolver(TemplateMode mode, String pattern, int order) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/mail/");
        resolver.setResolvablePatterns(Set.of(pattern));
        resolver.setTemplateMode(mode);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setOrder(order);
        resolver.setCheckExistence(true);
        resolver.setCacheable(true);
        return resolver;
    }
}
