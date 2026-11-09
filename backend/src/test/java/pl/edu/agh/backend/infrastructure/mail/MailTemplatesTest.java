package pl.edu.agh.backend.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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
    void registrationPromoted_skipsThePlaceOfAnOnlineEvent() {
        String html = mail.render("registration-promoted.html", "Temat", event(null));
        String text = mail.render("registration-promoted.txt", "Temat", event(null));

        assertThat(html).doesNotContain("Miejsce");
        assertThat(text).doesNotContain("Miejsce");
    }
}
