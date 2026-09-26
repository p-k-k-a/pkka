package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.application.Faculty;
import pl.edu.agh.backend.application.StudyType;
import pl.edu.agh.backend.event.*;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** No {@code @Transactional}: the announcement fires on commit, which a rolled-back test never reaches. */
@SpringBootTest(properties = "app.notifications.reminder-cron=-")
@Testcontainers
@Import(EventNotificationTest.TestSecurityBeans.class)
class EventNotificationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private ExpoPushClient expoPushClient;

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private EventReminderScheduler eventReminderScheduler;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** The announcement is @Async, so assertions wait for the executor rather than racing it. */
    private static final long SEND_TIMEOUT_MS = 5000;

    private Caller admin;

    @BeforeEach
    void setUp() {
        eventRegistrationRepository.deleteAll();
        deviceTokenRepository.deleteAll();
        reset(expoPushClient);
        when(expoPushClient.send(any())).thenReturn(new ExpoPushClient.Outcome(true, List.of()));
        admin = new Caller(UUID.randomUUID().toString(), Set.of("ADMIN"));
    }

    private User userWithDevice(String token, boolean approvedAlumn) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user = userRepository.save(user);
        if (approvedAlumn) {
            applicationRepository.save(Application.builder()
                    .applicant(user)
                    .status(ApplicationStatus.APPROVED)
                    .faculty(Faculty.WI)
                    .fieldOfStudy("Informatyka")
                    .studyType(StudyType.MASTER)
                    .graduationYear(2020)
                    .phoneNumber("+48123456789")
                    .reviewedAt(Instant.parse("2021-06-15T10:00:00Z"))
                    .build());
        }
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .installationId(UUID.randomUUID().toString())
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build());
        return user;
    }

    private EventRequest request(Audience audience, Instant startsAt, Integer reminderLeadTimeMinutes) {
        return new EventRequest(
                "Warsztaty",
                null,
                EventType.ONLINE,
                startsAt,
                startsAt.plus(2, ChronoUnit.HOURS),
                null,
                null,
                null,
                null,
                audience,
                null,
                reminderLeadTimeMinutes,
                Set.of());
    }

    @SuppressWarnings("unchecked")
    private List<ExpoPushMessage> captureSentMessages() {
        ArgumentCaptor<List<ExpoPushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient, timeout(SEND_TIMEOUT_MS)).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void creatingAPublicEvent_announcesToEveryRegisteredDevice() {
        userWithDevice("ExponentPushToken[alumn]", true);
        userWithDevice("ExponentPushToken[plain]", false);

        AdminEventResponse created = adminEventService.create(
                admin, request(Audience.PUBLIC, Instant.now().plus(7, ChronoUnit.DAYS), null));

        List<ExpoPushMessage> sent = captureSentMessages();
        assertThat(sent).hasSize(2);
        assertThat(sent).allSatisfy(message -> {
            assertThat(message.channelId()).isEqualTo("events");
            assertThat(message.title()).isEqualTo("Nowe wydarzenie");
            assertThat(message.data())
                    .containsEntry("type", "EVENT_ANNOUNCEMENT")
                    .containsEntry("targetId", created.id().toString());
        });
    }

    @Test
    void creatingAnAlumniOnlyEvent_skipsUsersWithoutAnApprovedApplication() {
        userWithDevice("ExponentPushToken[alumn]", true);
        userWithDevice("ExponentPushToken[plain]", false);

        adminEventService.create(
                admin, request(Audience.ALL_ALUMNI, Instant.now().plus(7, ChronoUnit.DAYS), null));

        assertThat(captureSentMessages()).extracting(ExpoPushMessage::to).containsExactly("ExponentPushToken[alumn]");
    }

    @Test
    void aTokenExpoCallsGone_isDeleted() {
        userWithDevice("ExponentPushToken[dead]", false);
        when(expoPushClient.send(any()))
                .thenReturn(new ExpoPushClient.Outcome(true, List.of("ExponentPushToken[dead]")));

        adminEventService.create(admin, request(Audience.PUBLIC, Instant.now().plus(7, ChronoUnit.DAYS), null));

        awaitUntil(() -> deviceTokenRepository.findAll().isEmpty());
    }

    private Event registerFor(Instant startsAt, Integer leadTimeMinutes) {
        return registerFor(startsAt, leadTimeMinutes, Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private Event registerFor(Instant startsAt, Integer leadTimeMinutes, Instant registeredAt) {
        User user = userWithDevice("ExponentPushToken[attendee]", false);
        Event event = eventRepository.save(Event.builder()
                .title("Warsztaty")
                .type(EventType.ONLINE)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .audience(Audience.PUBLIC)
                .reminderLeadTimeMinutes(leadTimeMinutes)
                .build());
        EventRegistration registration = eventRegistrationRepository.save(
                EventRegistration.builder().event(event).user(user).build());
        jdbcTemplate.update(
                "update event_registrations set registered_at = ? where id = ?",
                Timestamp.from(registeredAt),
                registration.getId());
        return event;
    }

    @Test
    void aDueReminder_isSentOnceAndThenMarked() {
        Event event = registerFor(Instant.now().plus(30, ChronoUnit.MINUTES), 60);

        eventReminderScheduler.sendDueReminders();

        List<ExpoPushMessage> sent = captureSentMessages();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().channelId()).isEqualTo("reminders");
        assertThat(sent.getFirst().title()).isEqualTo("Przypomnienie");
        assertThat(sent.getFirst().data()).containsEntry("type", "EVENT_REMINDER");
        assertThat(eventRegistrationRepository.findAll().stream()
                        .filter(r -> r.getEvent().getId().equals(event.getId()))
                        .allMatch(r -> r.getReminderSentAt() != null))
                .isTrue();

        reset(expoPushClient);
        when(expoPushClient.send(any())).thenReturn(new ExpoPushClient.Outcome(true, List.of()));
        eventReminderScheduler.sendDueReminders();
        verify(expoPushClient, never()).send(any());
    }

    /** Regression: a refused batch must leave the reminder due, not silently consume it. */
    @Test
    void aRefusedSend_leavesTheReminderDueForTheNextSweep() {
        registerFor(Instant.now().plus(30, ChronoUnit.MINUTES), 60);
        when(expoPushClient.send(any())).thenReturn(new ExpoPushClient.Outcome(false, List.of()));

        eventReminderScheduler.sendDueReminders();

        assertThat(eventRegistrationRepository.findAll())
                .allSatisfy(registration ->
                        assertThat(registration.getReminderSentAt()).isNull());

        reset(expoPushClient);
        when(expoPushClient.send(any())).thenReturn(new ExpoPushClient.Outcome(true, List.of()));
        eventReminderScheduler.sendDueReminders();
        verify(expoPushClient, timeout(SEND_TIMEOUT_MS)).send(any());
    }

    /**
     * Regression: deleting a dead token mid-sweep used to clear the persistence context, silently discarding
     * every reminderSentAt stamp and re-sending the same reminders every hour.
     */
    @Test
    void aDeadTokenDuringTheSweep_stillMarksTheReminderSent() {
        registerFor(Instant.now().plus(30, ChronoUnit.MINUTES), 60);
        when(expoPushClient.send(any()))
                .thenReturn(new ExpoPushClient.Outcome(true, List.of("ExponentPushToken[attendee]")));

        eventReminderScheduler.sendDueReminders();

        assertThat(deviceTokenRepository.findAll()).isEmpty();
        assertThat(eventRegistrationRepository.findAll())
                .allSatisfy(registration ->
                        assertThat(registration.getReminderSentAt()).isNotNull());
    }

    @Test
    void aReminderStillOutsideItsLeadTime_isNotSent() {
        registerFor(Instant.now().plus(5, ChronoUnit.DAYS), 60);

        eventReminderScheduler.sendDueReminders();

        verify(expoPushClient, never()).send(any());
    }

    @Test
    void aRegistrationInsideTheLeadTime_isNeverReminded() {
        registerFor(Instant.now().plus(30, ChronoUnit.MINUTES), 60, Instant.now());

        eventReminderScheduler.sendDueReminders();

        verify(expoPushClient, never()).send(any());
    }

    @Test
    void anEventWithoutALeadTime_neverReminds() {
        registerFor(Instant.now().plus(30, ChronoUnit.MINUTES), null);

        eventReminderScheduler.sendDueReminders();

        verify(expoPushClient, never()).send(any());
    }

    private static void awaitUntil(java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + SEND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("condition was not met within %d ms".formatted(SEND_TIMEOUT_MS));
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return mock(ClientRegistrationRepository.class);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }
}
