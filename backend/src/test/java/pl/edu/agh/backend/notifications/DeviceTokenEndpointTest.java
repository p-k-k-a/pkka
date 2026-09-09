package pl.edu.agh.backend.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(DeviceTokenEndpointTest.TestSecurityBeans.class)
class DeviceTokenEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private String keycloakId;
    private String installationId;

    @BeforeEach
    void setUp() {
        keycloakId = UUID.randomUUID().toString();
        installationId = UUID.randomUUID().toString();
    }

    private static String token(String suffix) {
        return "ExponentPushToken[%s]".formatted(suffix);
    }

    private static String body(String token) {
        return """
                {"token":"%s","platform":"ANDROID"}""".formatted(token);
    }

    private RequestPostProcessor user() {
        return user(keycloakId);
    }

    private RequestPostProcessor user(String subject) {
        return jwt().jwt(t -> t.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private User existingUser(String subject) {
        User user = new User();
        user.setKeycloakId(subject);
        return userRepository.save(user);
    }

    @Test
    void registerWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("aaa"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_storesTokenForCaller() throws Exception {
        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("aaa"))))
                .andExpect(status().isNoContent());

        DeviceToken stored =
                deviceTokenRepository.findByInstallationId(installationId).orElseThrow();
        assertThat(stored.getToken()).isEqualTo(token("aaa"));
        assertThat(stored.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(stored.getUser().getKeycloakId()).isEqualTo(keycloakId);
    }

    @Test
    void registerTwice_rotatesTokenInPlace() throws Exception {
        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("old"))))
                .andExpect(status().isNoContent());
        UUID firstId = deviceTokenRepository
                .findByInstallationId(installationId)
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("new"))))
                .andExpect(status().isNoContent());

        DeviceToken stored =
                deviceTokenRepository.findByInstallationId(installationId).orElseThrow();
        assertThat(stored.getId()).isEqualTo(firstId);
        assertThat(stored.getToken()).isEqualTo(token("new"));
        assertThat(deviceTokenRepository.count()).isEqualTo(1);
    }

    /** Android recycles a token onto a fresh install; the stale row must go or the device gets doubles. */
    @Test
    void registerWithTokenHeldByAnotherInstallation_dropsTheStaleRow() throws Exception {
        String otherInstallation = UUID.randomUUID().toString();
        deviceTokenRepository.save(DeviceToken.builder()
                .user(existingUser(UUID.randomUUID().toString()))
                .installationId(otherInstallation)
                .token(token("shared"))
                .platform(DevicePlatform.ANDROID)
                .build());

        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("shared"))))
                .andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByInstallationId(otherInstallation))
                .isEmpty();
        assertThat(deviceTokenRepository.findByInstallationId(installationId)).isPresent();
    }

    @Test
    void registerWithMalformedToken_isBadRequest() throws Exception {
        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("not-a-push-token")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unregister_removesTheRow() throws Exception {
        mockMvc.perform(put("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(token("aaa"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findByInstallationId(installationId)).isEmpty();
    }

    @Test
    void unregisterSomeoneElsesInstallation_isNotFound() throws Exception {
        deviceTokenRepository.save(DeviceToken.builder()
                .user(existingUser(UUID.randomUUID().toString()))
                .installationId(installationId)
                .token(token("theirs"))
                .platform(DevicePlatform.ANDROID)
                .build());

        mockMvc.perform(delete("/api/notifications/devices/{id}", installationId)
                        .with(user())
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertThat(deviceTokenRepository.findByInstallationId(installationId)).isPresent();
    }

    @Test
    void unregisterUnknownInstallation_isNotFound() throws Exception {
        mockMvc.perform(delete("/api/notifications/devices/{id}", UUID.randomUUID())
                        .with(user())
                        .with(csrf()))
                .andExpect(status().isNotFound());
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
