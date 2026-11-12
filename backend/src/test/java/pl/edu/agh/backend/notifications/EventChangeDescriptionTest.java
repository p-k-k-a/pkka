package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventScheduleChangedEvent;
import pl.edu.agh.backend.event.EventType;

class EventChangeDescriptionTest {

    private static final Instant STARTS_AT = Instant.parse("2030-12-01T17:00:00Z");

    private static Event event(EventType type, Instant startsAt, String location, String transmissionUrl) {
        return Event.builder()
                .title("Warsztaty")
                .slug("warsztaty")
                .type(type)
                .startsAt(startsAt)
                .location(location)
                .transmissionUrl(transmissionUrl)
                .audience(Audience.PUBLIC)
                .build();
    }

    private static EventScheduleChangedEvent before(
            EventType type, Instant startsAt, String location, String transmissionUrl) {
        return new EventScheduleChangedEvent(null, startsAt, null, type, location, transmissionUrl);
    }

    @Test
    void namesAChangeOfFormAndLink() {
        EventScheduleChangedEvent before = before(EventType.IN_PERSON, STARTS_AT, "D17", null);
        Event now = event(EventType.HYBRID, STARTS_AT, "D17", "https://meet.example.com/abc");

        assertThat(NotificationService.describeChange(now, before))
                .containsExactly("forma: hybrydowo", "nowy link do transmisji");
        assertThat(EventMailService.describeChanges(now, before))
                .containsExactly(
                        "Forma: stacjonarnie → hybrydowo",
                        "Zmienił się link do transmisji, aktualny jest na stronie wydarzenia");
    }

    @Test
    void aClearedPlaceIsNotCalledANewOne() {
        EventScheduleChangedEvent before = before(EventType.IN_PERSON, STARTS_AT, "D17", null);
        Event now = event(EventType.IN_PERSON, STARTS_AT, null, null);

        assertThat(NotificationService.describeChange(now, before)).containsExactly("miejsce do potwierdzenia");
        assertThat(EventMailService.describeChanges(now, before)).containsExactly("Miejsce: D17 → nie podano");
    }

    @Test
    void secondsSpacesAndBlanksAreNoChange() {
        EventScheduleChangedEvent before = before(EventType.ONLINE, STARTS_AT, " D17 ", "");
        Event now = event(EventType.ONLINE, STARTS_AT.plusSeconds(59), "D17", null);

        assertThat(before.anyChange(now)).isFalse();
        assertThat(NotificationService.describeChange(now, before)).isEmpty();
        assertThat(EventMailService.describeChanges(now, before)).isEmpty();
    }
}
