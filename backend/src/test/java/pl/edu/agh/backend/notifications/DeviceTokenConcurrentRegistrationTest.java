package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.notifications.dto.RegisterDeviceRequest;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@Testcontainers
@Import(DeviceTokenConcurrentRegistrationTest.TestSecurityBeans.class)
class DeviceTokenConcurrentRegistrationTest {

    private static final int ATTEMPTS = 20;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private Caller caller;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        userRepository.save(user);
        caller = new Caller(user.getKeycloakId(), Set.of("USER"));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        deviceTokenRepository.deleteAll();
        userRepository.findByKeycloakId(caller.keycloakId()).ifPresent(userRepository::delete);
    }

    @Test
    void simultaneousRegistrationsOfOneInstallation_bothSucceedWithASingleRow() throws Exception {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            String installationId = UUID.randomUUID().toString();
            RegisterDeviceRequest request = new RegisterDeviceRequest(
                    "ExponentPushToken[race-%s]".formatted(installationId), DevicePlatform.ANDROID);
            CountDownLatch start = new CountDownLatch(1);

            List<Future<?>> registrations = List.of(
                    executor.submit(() -> {
                        start.await();
                        deviceTokenService.register(caller, installationId, request);
                        return null;
                    }),
                    executor.submit(() -> {
                        start.await();
                        deviceTokenService.register(caller, installationId, request);
                        return null;
                    }));
            start.countDown();
            for (Future<?> registration : registrations) {
                registration.get();
            }

            assertThat(deviceTokenRepository.findByInstallationId(installationId))
                    .hasValueSatisfying(device -> assertThat(device.getToken()).isEqualTo(request.token()));
        }
        assertThat(deviceTokenRepository.count()).isEqualTo(ATTEMPTS);
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
