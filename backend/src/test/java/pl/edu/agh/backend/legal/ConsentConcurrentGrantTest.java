package pl.edu.agh.backend.legal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** Not transactional: each grant must commit on its own for the two to really race. */
@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
class ConsentConcurrentGrantTest {

    private static final int ATTEMPTS = 20;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ConsentRecordRepository consentRepository;

    @Autowired
    private LegalDocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private User user;
    private Caller caller;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        userRepository.save(user);
        caller = new Caller(user.getKeycloakId(), Set.of("USER"));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        userRepository.delete(user);
    }

    @Test
    void simultaneousGrantsOfOneVersion_bothSucceedWithOneActiveRecord() throws Exception {
        UUID newsletter = documentRepository
                .findCurrent(LegalDocumentType.NEWSLETTER_CONSENT, Instant.now())
                .orElseThrow()
                .getId();

        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> grants = List.of(
                    executor.submit(() -> {
                        start.await();
                        return consentService.grant(caller, newsletter);
                    }),
                    executor.submit(() -> {
                        start.await();
                        return consentService.grant(caller, newsletter);
                    }));
            start.countDown();
            for (Future<?> grant : grants) {
                grant.get();
            }

            assertThat(consentRepository.findActiveByUserId(user.getId())).hasSize(1);
            consentService.withdraw(caller, LegalDocumentType.NEWSLETTER_CONSENT);
        }

        assertThat(consentRepository.findAll().stream()
                        .filter(consent -> consent.getUser().getId().equals(user.getId())))
                .hasSize(ATTEMPTS);
    }
}
