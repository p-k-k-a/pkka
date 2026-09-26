package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest(properties = {"app.notifications.reminder-cron=-", "app.notifications.receipt-cron=-"})
@Testcontainers
@Import(PushReceiptCheckTest.TestSecurityBeans.class)
class PushReceiptCheckTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockitoBean
    private ExpoPushClient expoPushClient;

    @Autowired
    private PushReceiptChecker pushReceiptChecker;

    @Autowired
    private PushTicketRepository pushTicketRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        pushTicketRepository.deleteAll();
        deviceTokenRepository.deleteAll();
    }

    private void device(String token) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(userRepository.save(user))
                .installationId(UUID.randomUUID().toString())
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build());
    }

    private void ticket(String id, String token, Instant createdAt) {
        pushTicketRepository.save(
                PushTicket.builder().id(id).token(token).createdAt(createdAt).build());
    }

    private static Instant minutesAgo(long minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES);
    }

    @Test
    void aReceiptSayingTheDeviceIsGone_deletesItsTokenAndForgetsBothTickets() {
        device("ExponentPushToken[alive]");
        device("ExponentPushToken[gone]");
        ticket("ticket-alive", "ExponentPushToken[alive]", minutesAgo(20));
        ticket("ticket-gone", "ExponentPushToken[gone]", minutesAgo(20));
        when(expoPushClient.fetchReceipts(any()))
                .thenReturn(new ExpoPushClient.Receipts(Set.of("ticket-alive", "ticket-gone"), Set.of("ticket-gone")));

        pushReceiptChecker.checkReceipts();

        assertThat(deviceTokenRepository.findAll())
                .extracting(DeviceToken::getToken)
                .containsExactly("ExponentPushToken[alive]");
        assertThat(pushTicketRepository.findAll()).isEmpty();
    }

    @Test
    void aTicketWithoutAReceiptYet_isCheckedAgainNextTime() {
        ticket("ticket-pending", "ExponentPushToken[alive]", minutesAgo(20));
        when(expoPushClient.fetchReceipts(any())).thenReturn(new ExpoPushClient.Receipts(Set.of(), Set.of()));

        pushReceiptChecker.checkReceipts();

        assertThat(pushTicketRepository.findAll()).extracting(PushTicket::getId).containsExactly("ticket-pending");
    }

    @Test
    void aTicketYoungerThanFifteenMinutes_isNotCheckedYet() {
        ticket("ticket-fresh", "ExponentPushToken[alive]", minutesAgo(1));

        pushReceiptChecker.checkReceipts();

        verify(expoPushClient, never()).fetchReceipts(any());
        assertThat(pushTicketRepository.findAll()).hasSize(1);
    }

    @Test
    void aTicketOlderThanADay_isDroppedWithoutChecking() {
        ticket("ticket-expired", "ExponentPushToken[alive]", minutesAgo(25 * 60));

        pushReceiptChecker.checkReceipts();

        verify(expoPushClient, never()).fetchReceipts(any());
        assertThat(pushTicketRepository.findAll()).isEmpty();
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
