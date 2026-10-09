package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;

/** One transaction per request at the same time — hence a thread pool, and deliberately no {@code @Transactional}. */
@SpringBootTest
@Testcontainers
@Import(TestSecurityConfig.class)
class UserProvisioningConcurrencyTest {

    private static final int CONTENDERS = 8;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UserProvisioningService userProvisioningService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentFirstRequestsOfANewUserAllGetTheSameRow() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<UUID>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS)) {
            for (int i = 0; i < CONTENDERS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return userProvisioningService.getOrCreate(keycloakId).getId();
                }));
            }
            start.countDown();

            List<UUID> ids = new ArrayList<>();
            for (Future<UUID> result : results) {
                ids.add(result.get());
            }
            assertThat(ids).hasSize(CONTENDERS).containsOnly(ids.getFirst());
        }

        assertThat(userRepository.findByKeycloakId(keycloakId)).isPresent();
    }
}
