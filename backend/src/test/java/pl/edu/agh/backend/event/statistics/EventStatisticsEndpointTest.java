package pl.edu.agh.backend.event.statistics;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.registration.RegistrationActivity;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository;
import pl.edu.agh.backend.event.registration.RegistrationActivityType;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class EventStatisticsEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationActivityRepository activityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanEvents() {
        eventRepository.deleteAll();
    }

    private Event newEvent(String title, Integer seatLimit, Instant startsAt) {
        return eventRepository.saveAndFlush(Event.builder()
                .title(title)
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .seatLimit(seatLimit)
                .audience(Audience.PUBLIC)
                .build());
    }

    private Event upcomingEvent(Integer seatLimit) {
        return newEvent(
                "Wydarzenie " + UUID.randomUUID(), seatLimit, Instant.now().plus(7, ChronoUnit.DAYS));
    }

    private void publishedAt(Event event, Instant createdAt) {
        jdbcTemplate.update("UPDATE events SET created_at = ? WHERE id = ?", Timestamp.from(createdAt), event.getId());
        entityManager.clear();
    }

    private void activity(Event event, RegistrationActivityType type, Instant occurredAt) {
        activityRepository.saveAndFlush(RegistrationActivity.builder()
                .event(event)
                .type(type)
                .occurredAt(occurredAt)
                .build());
    }

    private String signUp(Event event) throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf()))
                .andExpect(status().isCreated());
        return keycloakId;
    }

    private void cancel(Event event, String keycloakId) throws Exception {
        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        Event event = upcomingEvent(10);

        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/events/statistics").with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownEvent_isNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/events/{id}/statistics", UUID.randomUUID())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void countsSeatsWaitlistAndBothKindsOfCancellation() throws Exception {
        Event event = upcomingEvent(2);
        String first = signUp(event);
        signUp(event);
        signUp(event);
        String queued = signUp(event);

        cancel(event, first);
        cancel(event, queued);

        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.seatLimit").value(2))
                .andExpect(jsonPath("$.registered").value(2))
                .andExpect(jsonPath("$.waitlisted").value(0))
                .andExpect(jsonPath("$.signUps").value(4))
                .andExpect(jsonPath("$.cancellations").value(2))
                .andExpect(jsonPath("$.cancellationsFromWaitlist").value(1))
                .andExpect(jsonPath("$.promotions").value(1))
                .andExpect(jsonPath("$.occupancyPercent").value(100));
    }

    @Test
    void occupancyIsRoundedAndAbsentWithoutASeatLimit() throws Exception {
        Event limited = upcomingEvent(3);
        signUp(limited);
        Event unlimited = upcomingEvent(null);
        signUp(unlimited);

        mockMvc.perform(get("/api/admin/events/{id}/statistics", limited.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.occupancyPercent").value(33));
        mockMvc.perform(get("/api/admin/events/{id}/statistics", unlimited.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.registered").value(1))
                .andExpect(jsonPath("$.occupancyPercent").value(nullValue()));
    }

    @Test
    void occupancyOfAZeroSeatEventIsFullAndNeverDividesByZero() throws Exception {
        Event event = upcomingEvent(0);
        signUp(event);

        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.registered").value(0))
                .andExpect(jsonPath("$.waitlisted").value(1))
                .andExpect(jsonPath("$.occupancyPercent").value(100));
    }

    @Test
    void dailySeriesRunsFromPublicationToTodayWithEmptyDaysFilledIn() throws Exception {
        Event event = upcomingEvent(50);
        Instant now = Instant.now();
        publishedAt(event, now.minus(3, ChronoUnit.DAYS));
        activity(event, RegistrationActivityType.SIGNED_UP, now.minus(3, ChronoUnit.DAYS));
        activity(event, RegistrationActivityType.WAITLISTED, now.minus(3, ChronoUnit.DAYS));
        activity(event, RegistrationActivityType.SIGNED_UP, now.minus(1, ChronoUnit.DAYS));
        activity(event, RegistrationActivityType.CANCELLED, now.minus(1, ChronoUnit.DAYS));
        activity(event, RegistrationActivityType.PROMOTED, now.minus(1, ChronoUnit.DAYS));

        LocalDate today = LocalDate.now(WARSAW);
        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.daily", hasSize(4)))
                .andExpect(jsonPath("$.daily[*].date")
                        .value(contains(
                                today.minusDays(3).toString(),
                                today.minusDays(2).toString(),
                                today.minusDays(1).toString(),
                                today.toString())))
                .andExpect(jsonPath("$.daily[*].signUps").value(contains(2, 0, 1, 0)))
                .andExpect(jsonPath("$.daily[*].cancellations").value(contains(0, 0, 1, 0)));
    }

    @Test
    void dailySeriesBucketsByWarsawCalendarDay() throws Exception {
        Event event = upcomingEvent(50);
        LocalDate today = LocalDate.now(WARSAW);
        Instant lateYesterdayInUtcButTodayInWarsaw =
                today.atStartOfDay(WARSAW).toInstant().plus(30, ChronoUnit.MINUTES);
        publishedAt(event, today.minusDays(1).atStartOfDay(WARSAW).toInstant());
        activity(event, RegistrationActivityType.SIGNED_UP, lateYesterdayInUtcButTodayInWarsaw);

        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.daily[*].date")
                        .value(contains(today.minusDays(1).toString(), today.toString())))
                .andExpect(jsonPath("$.daily[*].signUps").value(contains(0, 1)));
    }

    @Test
    void dailySeriesOfAPastEventEndsOnTheDayItStarted() throws Exception {
        Instant startsAt = Instant.now().minus(5, ChronoUnit.DAYS);
        Event event = newEvent("Minione", 10, startsAt);
        publishedAt(event, startsAt.minus(2, ChronoUnit.DAYS));

        LocalDate startDay = LocalDate.ofInstant(startsAt, WARSAW);
        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.daily[*].date")
                        .value(contains(
                                startDay.minusDays(2).toString(),
                                startDay.minusDays(1).toString(),
                                startDay.toString())));
    }

    @Test
    void dailySeriesStretchesToCoverActivityRecordedBeforePublication() throws Exception {
        Event event = upcomingEvent(10);
        Instant now = Instant.now();
        activity(event, RegistrationActivityType.SIGNED_UP, now.minus(2, ChronoUnit.DAYS));

        LocalDate today = LocalDate.now(WARSAW);
        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.daily", hasSize(3)))
                .andExpect(jsonPath("$.daily[0].date").value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.daily[0].signUps").value(1));
    }

    @Test
    void comparisonListsEveryEventNewestFirstWithItsCounts() throws Exception {
        Event small = newEvent("Mały warsztat", 2, Instant.now().plus(2, ChronoUnit.DAYS));
        signUp(small);
        signUp(small);
        String queued = signUp(small);
        cancel(small, queued);
        Event big = newEvent("Gala", null, Instant.now().plus(9, ChronoUnit.DAYS));
        signUp(big);
        Event past = newEvent("Minione", 20, Instant.now().minus(3, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/admin/events/statistics").with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(contains("Gala", "Mały warsztat", "Minione")))
                .andExpect(jsonPath("$[1].eventId").value(small.getId().toString()))
                .andExpect(jsonPath("$[1].registered").value(2))
                .andExpect(jsonPath("$[1].waitlisted").value(0))
                .andExpect(jsonPath("$[1].cancellations").value(1))
                .andExpect(jsonPath("$[1].seatLimit").value(2))
                .andExpect(jsonPath("$[1].occupancyPercent").value(100))
                .andExpect(jsonPath("$[0].occupancyPercent").value(nullValue()))
                .andExpect(jsonPath("$[2].registered").value(0))
                .andExpect(jsonPath("$[2].occupancyPercent").value(0));

        mockMvc.perform(get("/api/admin/events/statistics")
                        .param("timeframe", "UPCOMING")
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[*].title").value(contains("Gala", "Mały warsztat")));
        mockMvc.perform(get("/api/admin/events/statistics")
                        .param("timeframe", "PAST")
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[*].eventId").value(contains(past.getId().toString())));
    }

    @Test
    void comparisonLeavesOutDeletedEvents() throws Exception {
        Event kept = upcomingEvent(10);
        Event deleted = upcomingEvent(10);
        eventRepository.delete(deleted);
        eventRepository.flush();

        mockMvc.perform(get("/api/admin/events/statistics").with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[*].eventId").value(contains(kept.getId().toString())));
    }

    @Test
    void occupancyAboveTheLimitIsReportedAsItIs() throws Exception {
        Event event = upcomingEvent(4);
        signUp(event);
        signUp(event);
        signUp(event);
        jdbcTemplate.update("UPDATE events SET seat_limit = 2 WHERE id = ?", event.getId());
        entityManager.clear();

        mockMvc.perform(get("/api/admin/events/{id}/statistics", event.getId()).with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$.registered").value(3))
                .andExpect(jsonPath("$.occupancyPercent").value(150));
    }
}
