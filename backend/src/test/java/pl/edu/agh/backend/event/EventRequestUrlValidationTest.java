package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventRequestUrlValidationTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsAllowedMeetingProvidersAndCoverUrls() {
        assertThat(violations("https://meet.google.com/abc-defg-hij", "https://files.pkka.me/covers/events/a.jpg"))
                .isEmpty();
        assertThat(violations("https://teams.microsoft.com/l/meetup-join/19%3ameeting", "http://localhost:9000/a.jpg"))
                .isEmpty();
        assertThat(violations("https://us02web.zoom.us/j/123456789", null)).isEmpty();
    }

    @Test
    void acceptsTheObjectKeyOfAnUploadedCoverButNoOtherKeys() {
        assertThat(violations(null, "covers/events/0f3a1c8e-1111-4000-8000-000000000001.png"))
                .isEmpty();
        assertThat(violations(null, "materials/0f3a1c8e-1111-4000-8000-000000000001.pdf"))
                .hasSize(1);
        assertThat(violations(null, "avatars/x/y.png")).hasSize(1);
        assertThat(violations("covers/events/a.png", null)).hasSize(1);
    }

    @Test
    void acceptsMissingAndBlankUrls() {
        assertThat(violations(null, null)).isEmpty();
        assertThat(violations("   ", "")).isEmpty();
    }

    @Test
    void rejectsOtherSchemesAndNonProviderMeetingLinks() {
        assertThat(violations("javascript:alert(1)", "data:image/png;base64,AAAA"))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("transmissionUrl", "coverImageUrl");
        assertThat(violations("https://meet.example.com/x", null)).hasSize(1);
        assertThat(violations("http://meet.google.com/abc-defg-hij", null)).hasSize(1);
        assertThat(violations("ftp://example.com/x", null)).hasSize(1);
        assertThat(violations("meet.example.com", null)).hasSize(1);
    }

    private static Set<ConstraintViolation<EventRequest>> violations(String transmissionUrl, String coverImageUrl) {
        Instant start = Instant.parse("2030-01-01T10:00:00Z");
        EventRequest request = new EventRequest(
                "Wydarzenie",
                null,
                EventType.ONLINE,
                start,
                start.plusSeconds(3600),
                transmissionUrl,
                null,
                null,
                null,
                Audience.PUBLIC,
                coverImageUrl,
                null,
                null);
        return VALIDATOR.validate(request);
    }
}
