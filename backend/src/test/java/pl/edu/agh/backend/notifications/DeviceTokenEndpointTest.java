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
import org.springframework.test.web.servlet.ResultActions;
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

    @BeforeEach
    void setUp() {
        keycloakId = UUID.randomUUID().toString();
    }

    private static String token(String suffix) {
        return "ExponentPushToken[%s]".formatted(suffix);
    }

    private static final String BODY = """
            {"platform":"ANDROID"}""";

    private RequestPostProcessor user() {
        return jwt().jwt(t -> t.subject(keycloakId)).authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private ResultActions register(String token) throws Exception {
        return mockMvc.perform(put("/api/notifications/tokens/{token}", token)
                .with(user())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY));
    }

    private ResultActions unregister(String token) throws Exception {
        return mockMvc.perform(
                delete("/api/notifications/tokens/{token}", token).with(user()).with(csrf()));
    }

    private void deviceOwnedBy(String ownerKeycloakId, String token) {
        User owner = new User();
        owner.setKeycloakId(ownerKeycloakId);
        deviceTokenRepository.save(DeviceToken.builder()
                .user(userRepository.save(owner))
                .token(token)
                .platform(DevicePlatform.ANDROID)
                .build());
    }

    @Test
    void registerWithoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(put("/api/notifications/tokens/{token}", token("aaa"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_storesTokenForCaller() throws Exception {
        register(token("aaa")).andExpect(status().isNoContent());

        DeviceToken stored = deviceTokenRepository.findById(token("aaa")).orElseThrow();
        assertThat(stored.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(stored.getUser().getKeycloakId()).isEqualTo(keycloakId);
    }

    @Test
    void registerTwice_keepsOneRow() throws Exception {
        register(token("aaa")).andExpect(status().isNoContent());
        register(token("aaa")).andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.count()).isEqualTo(1);
    }

    @Test
    void registerWithTokenHeldByAnotherUser_isConflict() throws Exception {
        String owner = UUID.randomUUID().toString();
        deviceOwnedBy(owner, token("theirs"));

        register(token("theirs")).andExpect(status().isConflict());

        assertThat(deviceTokenRepository
                        .findById(token("theirs"))
                        .orElseThrow()
                        .getUser()
                        .getKeycloakId())
                .isEqualTo(owner);
    }

    @Test
    void unregister_removesTheRow() throws Exception {
        register(token("aaa")).andExpect(status().isNoContent());

        unregister(token("aaa")).andExpect(status().isNoContent());

        assertThat(deviceTokenRepository.findById(token("aaa"))).isEmpty();
    }

    @Test
    void unregisterSomeoneElsesToken_isNotFound() throws Exception {
        deviceOwnedBy(UUID.randomUUID().toString(), token("theirs"));

        unregister(token("theirs")).andExpect(status().isNotFound());

        assertThat(deviceTokenRepository.findById(token("theirs"))).isPresent();
    }

    @Test
    void unregisterUnknownToken_isNotFound() throws Exception {
        unregister(token("unknown")).andExpect(status().isNotFound());
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
