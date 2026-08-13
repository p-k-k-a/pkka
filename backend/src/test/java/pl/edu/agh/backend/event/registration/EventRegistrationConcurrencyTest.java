package pl.edu.agh.backend.event.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/**
 * A seat limit has to hold under concurrent sign-ups, which needs a transaction per contender running at
 * the same time — hence a thread pool instead of MockMvc, and deliberately no {@code @Transactional}.
 */
@SpringBootTest
@Testcontainers
@Import(EventRegistrationConcurrencyTest.TestSecurityBeans.class)
class EventRegistrationConcurrencyTest {

    private static final int SEAT_LIMIT = 3;
    private static final int CONTENDERS = 8;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    private Event event;
    private final List<User> contenders = new ArrayList<>();

    /** Nothing runs in a test transaction here, so the committed rows go by hand. */
    @AfterEach
    void cleanUp() {
        eventRegistrationRepository.deleteAll();
        if (event != null) {
            eventRepository.delete(event);
        }
        userRepository.deleteAll(contenders);
    }

    @Test
    void concurrentRegistrations_neverOversellTheEvent() throws Exception {
        event = eventRepository.save(Event.builder()
                .title("Concurrency test event")
                .type(EventType.IN_PERSON)
                .startsAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .endsAt(Instant.now().plus(7, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .seatLimit(SEAT_LIMIT)
                .audience(Audience.PUBLIC)
                .build());

        List<Authentication> authentications = new ArrayList<>();
        for (int i = 0; i < CONTENDERS; i++) {
            User user = new User();
            user.setKeycloakId(UUID.randomUUID().toString());
            contenders.add(userRepository.save(user));
            authentications.add(verifiedAlumn(user.getKeycloakId()));
        }

        // One latch for all of them, so the sign-ups actually overlap instead of trickling in.
        CountDownLatch startLine = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
        try {
            List<Future<Boolean>> attempts = new ArrayList<>();
            for (Authentication authentication : authentications) {
                attempts.add(pool.submit(() -> {
                    startLine.await();
                    try {
                        eventRegistrationService.register(event.getId(), authentication);
                        return true;
                    } catch (EventRegistrationConflictException ex) {
                        assertThat(ex.getReason()).isEqualTo(EventRegistrationConflictException.Reason.NO_SEATS_LEFT);
                        return false;
                    }
                }));
            }
            startLine.countDown();

            long seatsTaken = 0;
            for (Future<Boolean> attempt : attempts) {
                if (attempt.get(30, TimeUnit.SECONDS)) {
                    seatsTaken++;
                }
            }

            assertThat(seatsTaken).isEqualTo(SEAT_LIMIT);
            assertThat(eventRegistrationRepository.countByEventId(event.getId()))
                    .isEqualTo(SEAT_LIMIT);
        } finally {
            pool.shutdownNow();
        }
    }

    private static Authentication verifiedAlumn(String keycloakId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(keycloakId)
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN")));
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
