package pl.edu.agh.backend.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Renders the real templates, so a typo in a variable or a fragment fails here rather than in production. */
class MailTemplatesTest {

    private final MailService mail = new MailService(null, null, MailConfig.templateEngine(), "https://pkka.me");

    private static Map<String, Object> event(String location) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("title", "Warsztaty");
        variables.put("slug", "warsztaty");
        variables.put("startsAt", "1 grudnia 2030 o 18:00");
        variables.put("location", location);
        return variables;
    }

    @Test
    void registrationPromoted_linksToTheEventAndNamesThePlace() {
        String html = mail.render("registration-promoted.html", "Zwolniło się miejsce: Warsztaty", event("D17"));
        String text = mail.render("registration-promoted.txt", "Zwolniło się miejsce: Warsztaty", event("D17"));

        assertThat(html)
                .contains("Warsztaty", "1 grudnia 2030 o 18:00", "D17")
                .contains("href=\"https://pkka.me/events/warsztaty\"")
                .contains("Klub Alumnów WI AGH");
        assertThat(text)
                .contains("Wydarzenie: Warsztaty", "Termin: 1 grudnia 2030 o 18:00", "Miejsce: D17")
                .contains("https://pkka.me/events/warsztaty")
                .contains("Odpowiedz na tę wiadomość");
    }

    @Test
    void eventChanged_listsEveryChange() {
        Map<String, Object> variables = event("D17");
        variables.put("changes", List.of("Termin: A → B", "Miejsce: C → D"));

        String html = mail.render("event-changed.html", "Zmiana w wydarzeniu: Warsztaty", variables);
        String text = mail.render("event-changed.txt", "Zmiana w wydarzeniu: Warsztaty", variables);

        assertThat(html).contains("<li", "Termin: A → B", "Miejsce: C → D", "https://pkka.me/events/warsztaty");
        assertThat(text).contains("- Termin: A → B", "- Miejsce: C → D", "https://pkka.me/events/warsztaty");
    }

    @Test
    void eventCancelled_namesTheEventAndItsDate() {
        String html = mail.render("event-cancelled.html", "Wydarzenie odwołane: Warsztaty", event(null));
        String text = mail.render("event-cancelled.txt", "Wydarzenie odwołane: Warsztaty", event(null));

        assertThat(html).contains("Warsztaty", "1 grudnia 2030 o 18:00", "zostało odwołane");
        assertThat(text).contains("„Warsztaty”", "1 grudnia 2030 o 18:00", "https://pkka.me/events");
    }

    @Test
    void eventMessage_keepsTheAdminsTextButEscapesIt() {
        Map<String, Object> variables = event(null);
        variables.put("message", "Sala 2.41.\n<script>alert(1)</script>");
        variables.put("messageLines", List.of("Sala 2.41.", "<script>alert(1)</script>"));

        String html = mail.render("event-message.html", "Zmiana sali", variables);
        String text = mail.render("event-message.txt", "Zmiana sali", variables);

        assertThat(html)
                .contains("Zmiana sali", "Sala 2.41.<br/>&lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>", "pre-line");
        assertThat(text).contains("Zmiana sali", "Sala 2.41.\n");
    }

    @Test
    void registrationPromoted_skipsThePlaceOfAnOnlineEvent() {
        String html = mail.render("registration-promoted.html", "Temat", event(null));
        String text = mail.render("registration-promoted.txt", "Temat", event(null));

        assertThat(html).doesNotContain("Miejsce");
        assertThat(text).doesNotContain("Miejsce");
    }
}
