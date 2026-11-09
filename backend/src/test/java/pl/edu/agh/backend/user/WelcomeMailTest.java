package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;
import pl.edu.agh.backend.infrastructure.mail.MailService;

/** No {@code @Transactional}: the welcome mail goes out on commit, which a rolled-back test never reaches. */
@SpringBootTest(properties = {"app.notifications.reminder-cron=-", "app.notifications.receipt-cron=-"})
@Testcontainers
@Import(WelcomeMailTest.TestSecurityBeans.class)
class WelcomeMailTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final long SEND_TIMEOUT_MS = 5000;
    private static final long QUIET_PERIOD_MS = 500;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private KeycloakUserService keycloakUserService;

    @Autowired
    private UserProvisioningService userProvisioningService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        reset(mailService, keycloakUserService);
        when(mailService.isEnabled()).thenReturn(true);
        when(mailService.send(anyString(), anyString(), anyString(), anyMap())).thenReturn(true);
        when(keycloakUserService.fetchDiscordId(anyString())).thenReturn(Optional.empty());
    }

    private Object welcomedAt(String keycloakId) {
        return jdbcTemplate.queryForObject(
                "select welcome_mail_sent_at from users where keycloak_id = ?", Object.class, keycloakId);
    }

    @Test
    void aNewAccount_isWelcomedOnceAcrossLogins() {
        String keycloakId = UUID.randomUUID().toString();

        userProvisioningService.syncIdentityFromClaims(keycloakId, "Ala", "Nowak", "ala@example.com");

        verify(mailService, timeout(SEND_TIMEOUT_MS))
                .send(eq("ala@example.com"), eq("Witamy w PKKA"), eq("welcome"), anyMap());
        assertThat(welcomedAt(keycloakId)).isNotNull();

        userProvisioningService.syncIdentityFromClaims(keycloakId, "Ala", "Nowak", "ala@example.com");

        verify(mailService, after(QUIET_PERIOD_MS).times(1)).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void aFailedWelcome_isRetriedOnTheNextLogin() {
        when(mailService.send(anyString(), anyString(), eq("welcome"), anyMap()))
                .thenReturn(false)
                .thenReturn(true);
        String keycloakId = UUID.randomUUID().toString();

        userProvisioningService.syncIdentityFromClaims(keycloakId, "Ala", "Nowak", "ala@example.com");
        verify(mailService, timeout(SEND_TIMEOUT_MS).times(1)).send(anyString(), anyString(), anyString(), anyMap());
        awaitWelcomedAt(keycloakId, false);

        userProvisioningService.syncIdentityFromClaims(keycloakId, "Ala", "Nowak", "ala@example.com");
        verify(mailService, timeout(SEND_TIMEOUT_MS).times(2)).send(anyString(), anyString(), anyString(), anyMap());
        awaitWelcomedAt(keycloakId, true);
    }

    @Test
    void anAccountThatWasAlreadyWelcomed_isLeftAlone() {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setEmail("ola@example.com");
        userRepository.save(user);
        jdbcTemplate.update("update users set welcome_mail_sent_at = now() where id = ?", user.getId());

        userProvisioningService.syncIdentityFromClaims(user.getKeycloakId(), "Ola", "Kowalska", "ola@example.com");

        verify(mailService, after(QUIET_PERIOD_MS).never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    private void awaitWelcomedAt(String keycloakId, boolean welcomed) {
        long deadline = System.currentTimeMillis() + SEND_TIMEOUT_MS;
        while ((welcomedAt(keycloakId) != null) != welcomed) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("welcome_mail_sent_at did not become " + (welcomed ? "set" : "empty"));
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
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
