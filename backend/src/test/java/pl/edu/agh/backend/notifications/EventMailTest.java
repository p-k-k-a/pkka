package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.AdminEventService;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventRequest;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.message.AdminEventMessageService;
import pl.edu.agh.backend.event.message.EventMessageRecipientsResponse;
import pl.edu.agh.backend.event.message.EventMessageRequest;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
import pl.edu.agh.backend.notifications.expo.ExpoPushMessage;
import pl.edu.agh.backend.notifications.expo.SendOutcome;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** No {@code @Transactional}: mails go out on commit, which a rolled-back test never reaches. */
@SpringBootTest(properties = {"app.notifications.reminder-cron=-", "app.notifications.receipt-cron=-"})
@Testcontainers
@Import(EventMailTest.TestSecurityBeans.class)
class EventMailTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    /** Mails are sent by an @Async listener, so assertions wait for the executor rather than racing it. */
    private static final long SEND_TIMEOUT_MS = 5000;

    private static final long QUIET_PERIOD_MS = 500;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private ExpoPushClient expoPushClient;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private AdminEventService adminEventService;

    @Autowired
    private AdminEventMessageService adminEventMessageService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @BeforeEach
    void setUp() {
        eventRegistrationRepository.deleteAll();
        deviceTokenRepository.deleteAll();
        reset(mailService, expoPushClient);
        when(mailService.send(anyString(), anyString(), anyString(), anyMap())).thenReturn(true);
        when(expoPushClient.send(any())).thenReturn(new SendOutcome(Map.of(), Set.of()));
    }

    private User saveUser(String email) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setEmail(email);
        return userRepository.save(user);
    }

    private Event fullEventHeldBy(User seatHolder) {
        Event event = eventRepository.save(Event.builder()
                .title("Warsztaty")
                .slug("warsztaty-" + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(Instant.parse("2030-12-01T17:00:00Z"))
                .endsAt(Instant.parse("2030-12-01T19:00:00Z"))
                .location("D17, sala 1.38")
                .audience(Audience.PUBLIC)
                .seatLimit(1)
                .build());
        register(event, seatHolder, EventRegistrationStatus.REGISTERED);
        return event;
    }

    private void register(Event event, User user, EventRegistrationStatus status) {
        eventRegistrationRepository.save(EventRegistration.builder()
                .event(event)
                .user(user)
                .status(status)
                .build());
    }

    private void giveUpSeat(Event event, User seatHolder) {
        eventRegistrationService.unregister(event.getId(), new Caller(seatHolder.getKeycloakId(), Set.of()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFreedSeat_mailsTheNextPersonOnTheWaitlist() {
        User seatHolder = saveUser("holder@example.com");
        Event event = fullEventHeldBy(seatHolder);
        register(event, saveUser("waiting@example.com"), EventRegistrationStatus.WAITLISTED);

        giveUpSeat(event, seatHolder);

        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(
                        eq("waiting@example.com"),
                        eq("Zwolniło się miejsce: Warsztaty"),
                        eq("registration-promoted"),
                        variables.capture());
        assertThat(variables.getValue())
                .containsEntry("title", "Warsztaty")
                .containsEntry("slug", event.getSlug())
                .containsEntry("startsAt", "1 grudnia 2030 o 18:00")
                .containsEntry("location", "D17, sala 1.38");
    }

    @Test
    void aPromotionIsMailedEvenWhenThePushFails() {
        when(expoPushClient.send(any())).thenThrow(new IllegalStateException("Expo is down"));
        User seatHolder = saveUser("holder@example.com");
        Event event = fullEventHeldBy(seatHolder);
        register(event, saveUser("waiting@example.com"), EventRegistrationStatus.WAITLISTED);

        giveUpSeat(event, seatHolder);

        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(eq("waiting@example.com"), anyString(), eq("registration-promoted"), anyMap());
    }

    @Test
    void aPromotedUserWithoutAnAddress_getsNoMail() {
        User seatHolder = saveUser("holder@example.com");
        Event event = fullEventHeldBy(seatHolder);
        register(event, saveUser(null), EventRegistrationStatus.WAITLISTED);

        giveUpSeat(event, seatHolder);

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void aRolledBackPromotion_mailsNobody() {
        User seatHolder = saveUser("holder@example.com");
        Event event = fullEventHeldBy(seatHolder);
        register(event, saveUser("waiting@example.com"), EventRegistrationStatus.WAITLISTED);

        transactionTemplate.executeWithoutResult(transaction -> {
            giveUpSeat(event, seatHolder);
            transaction.setRollbackOnly();
        });

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    /** A full event: one seat taken, one person waiting, both with an address and a device. */
    private Event eventWithSeatAndWaitlist(Instant startsAt) {
        Event event = eventRepository.save(Event.builder()
                .title("Warsztaty")
                .slug("warsztaty-" + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .location("D17, sala 1.38")
                .audience(Audience.PUBLIC)
                .seatLimit(1)
                .build());
        register(
                event,
                userWithDevice("seated@example.com", "ExponentPushToken[seated]"),
                EventRegistrationStatus.REGISTERED);
        register(
                event,
                userWithDevice("waiting@example.com", "ExponentPushToken[waiting]"),
                EventRegistrationStatus.WAITLISTED);
        return event;
    }

    private User userWithDevice(String email, String token) {
        User user = saveUser(email);
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build());
        return user;
    }

    private static EventRequest edit(Event event, Instant startsAt, String location, String description) {
        return new EventRequest(
                event.getTitle(),
                description,
                EventType.IN_PERSON,
                startsAt,
                startsAt.plus(2, ChronoUnit.HOURS),
                null,
                location,
                1,
                null,
                Audience.PUBLIC,
                null,
                null,
                Set.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> captureMails(String template, int count) {
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> variables = ArgumentCaptor.forClass(Map.class);
        verify(mailService, timeout(SEND_TIMEOUT_MS).times(count))
                .send(to.capture(), anyString(), eq(template), variables.capture());
        Map<String, Map<String, Object>> byRecipient = new HashMap<>();
        for (int i = 0; i < count; i++) {
            byRecipient.put(to.getAllValues().get(i), variables.getAllValues().get(i));
        }
        return byRecipient;
    }

    @SuppressWarnings("unchecked")
    private List<ExpoPushMessage> capturePushes() {
        ArgumentCaptor<List<ExpoPushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(expoPushClient, timeout(SEND_TIMEOUT_MS)).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void movingAnEvent_mailsAndPushesEveryoneSignedUp() {
        Event event = eventWithSeatAndWaitlist(Instant.parse("2030-12-01T17:00:00Z"));

        adminEventService.update(
                event.getId(), edit(event, Instant.parse("2030-12-02T17:00:00Z"), "D17, sala 2.41", null));

        Map<String, Map<String, Object>> mails = captureMails("event-changed", 2);
        assertThat(mails).containsOnlyKeys("seated@example.com", "waiting@example.com");
        assertThat((List<String>) mails.get("waiting@example.com").get("changes"))
                .containsExactly(
                        "Termin: 1 grudnia 2030, 18:00 - 20:00 -> 2 grudnia 2030, 18:00 - 20:00",
                        "Miejsce: D17, sala 1.38 -> D17, sala 2.41");
        List<ExpoPushMessage> pushes = capturePushes();
        assertThat(pushes)
                .extracting(ExpoPushMessage::to)
                .containsExactlyInAnyOrder("ExponentPushToken[seated]", "ExponentPushToken[waiting]");
        assertThat(pushes.getFirst().body())
                .isEqualTo("„Warsztaty”: nowy termin 02.12 o 18:00; nowe miejsce: D17, sala 2.41");
        assertThat(pushes.getFirst().data()).containsEntry("type", "EVENT_CHANGED");
    }

    @Test
    void editingOnlyTheDescription_tellsNobody() {
        Event event = eventWithSeatAndWaitlist(Instant.parse("2030-12-01T17:00:00Z"));

        adminEventService.update(event.getId(), edit(event, event.getStartsAt(), event.getLocation(), "Nowy opis"));

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
        verify(expoPushClient, never()).send(any());
    }

    @Test
    void movingAnEventBySecondsOnly_tellsNobody() {
        Event event = eventWithSeatAndWaitlist(Instant.parse("2030-12-01T17:00:00Z"));

        adminEventService.update(
                event.getId(), edit(event, event.getStartsAt().plusSeconds(30), event.getLocation(), null));

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
        verify(expoPushClient, never()).send(any());
    }

    @Test
    void fixingAPastEvent_tellsNobody() {
        Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Event event = eventWithSeatAndWaitlist(lastWeek);

        adminEventService.update(event.getId(), edit(event, lastWeek.plus(1, ChronoUnit.HOURS), "D17", null));

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void deletingAnUpcomingEvent_tellsEveryoneItIsCancelled() {
        Event event = eventWithSeatAndWaitlist(Instant.parse("2030-12-01T17:00:00Z"));

        adminEventService.delete(event.getId());

        Map<String, Map<String, Object>> mails = captureMails("event-cancelled", 2);
        assertThat(mails).containsOnlyKeys("seated@example.com", "waiting@example.com");
        assertThat(mails.get("seated@example.com"))
                .containsEntry("title", "Warsztaty")
                .containsEntry("startsAt", "1 grudnia 2030 o 18:00");
        List<ExpoPushMessage> pushes = capturePushes();
        assertThat(pushes).hasSize(2);
        assertThat(pushes.getFirst().title()).isEqualTo("Wydarzenie odwołane");
        assertThat(pushes.getFirst().data())
                .containsEntry("type", "EVENT_CANCELLED")
                .containsEntry("target", "");
    }

    @Test
    void deletingAPastEvent_tellsNobody() {
        Event event = eventWithSeatAndWaitlist(Instant.now().minus(7, ChronoUnit.DAYS));

        adminEventService.delete(event.getId());

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void anAdminMessage_reachesTheWaitlistToo() {
        Event event = eventWithSeatAndWaitlist(Instant.parse("2030-12-01T17:00:00Z"));

        EventMessageRecipientsResponse queued = adminEventMessageService.send(
                event.getId(), new EventMessageRequest("  Zmiana sali ", "Spotykamy się w 2.41.\nWeźcie laptopy."));

        assertThat(queued.recipients()).isEqualTo(2);
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        verify(mailService, timeout(SEND_TIMEOUT_MS).times(2))
                .send(to.capture(), eq("Zmiana sali"), eq("event-message"), anyMap());
        assertThat(to.getAllValues()).containsExactlyInAnyOrder("seated@example.com", "waiting@example.com");
        ExpoPushMessage push = capturePushes().getFirst();
        assertThat(push.title()).isEqualTo("Warsztaty");
        assertThat(push.body()).isEqualTo("Zmiana sali");
        assertThat(push.data()).containsEntry("type", "EVENT_MESSAGE");
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
