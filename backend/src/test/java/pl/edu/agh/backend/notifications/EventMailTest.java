package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.infrastructure.mail.MailService;
import pl.edu.agh.backend.notifications.expo.ExpoPushClient;
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

    @BeforeEach
    void setUp() {
        eventRegistrationRepository.deleteAll();
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
