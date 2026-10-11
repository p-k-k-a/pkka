package pl.edu.agh.backend.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTag;
import pl.edu.agh.backend.user.UserTagRepository;
import pl.edu.agh.backend.user.UserTagResponse;

@SpringBootTest
@Testcontainers
@Import(ProfileTagsConcurrencyTest.TestSecurityBeans.class)
class ProfileTagsConcurrencyTest {

    private static final String TAG = "Concurrency Tag";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private ProfileService profileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTagRepository userTagRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private User lastHolder;
    private User newcomer;

    @BeforeEach
    void setUp() {
        lastHolder = newUser();
        newcomer = newUser();
        profileService.updateTags(lastHolder.getKeycloakId(), Set.of(), Set.of(TAG));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        for (User user : List.of(lastHolder, newcomer)) {
            profileService.updateTags(user.getKeycloakId(), Set.of(), Set.of());
            userRepository.deleteById(user.getId());
        }
    }

    private User newUser() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        return userRepository.save(user);
    }

    private List<String> tagsOf(User user) {
        return profileService.getTags(user.getKeycloakId()).stream()
                .map(UserTagResponse::name)
                .toList();
    }

    private List<String> catalogNames() {
        return userTagRepository.findAll().stream().map(UserTag::getName).toList();
    }

    @Test
    void removingTheLastUseWaitsForAConcurrentAddAndThenKeepsTheTag() throws Exception {
        CountDownLatch added = new CountDownLatch(1);
        CountDownLatch mayCommit = new CountDownLatch(1);
        Future<?> adding = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            profileService.updateTags(newcomer.getKeycloakId(), Set.of(), Set.of(TAG));
            userRepository.flush();
            added.countDown();
            awaitQuietly(mayCommit);
        }));
        assertThat(added.await(10, TimeUnit.SECONDS)).isTrue();

        ExecutorService remover = Executors.newSingleThreadExecutor();
        try {
            Future<?> removing =
                    remover.submit(() -> profileService.updateTags(lastHolder.getKeycloakId(), Set.of(), Set.of()));
            assertThatThrownBy(() -> removing.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            mayCommit.countDown();
            adding.get(10, TimeUnit.SECONDS);
            removing.get(10, TimeUnit.SECONDS);
        } finally {
            remover.shutdownNow();
        }

        assertThat(tagsOf(newcomer)).containsExactly(TAG);
        assertThat(tagsOf(lastHolder)).isEmpty();
    }

    @Test
    void addingATagThatIsBeingRemovedRecreatesIt() throws Exception {
        CountDownLatch removed = new CountDownLatch(1);
        CountDownLatch mayCommit = new CountDownLatch(1);
        Future<?> removing = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            profileService.updateTags(lastHolder.getKeycloakId(), Set.of(), Set.of());
            removed.countDown();
            awaitQuietly(mayCommit);
        }));
        assertThat(removed.await(10, TimeUnit.SECONDS)).isTrue();

        ExecutorService adder = Executors.newSingleThreadExecutor();
        try {
            Future<?> adding =
                    adder.submit(() -> profileService.updateTags(newcomer.getKeycloakId(), Set.of(), Set.of(TAG)));
            assertThatThrownBy(() -> adding.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);

            mayCommit.countDown();
            removing.get(10, TimeUnit.SECONDS);
            adding.get(10, TimeUnit.SECONDS);
        } finally {
            adder.shutdownNow();
        }

        assertThat(tagsOf(newcomer)).containsExactly(TAG);
        assertThat(catalogNames()).containsOnlyOnce(TAG);
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to commit");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
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
