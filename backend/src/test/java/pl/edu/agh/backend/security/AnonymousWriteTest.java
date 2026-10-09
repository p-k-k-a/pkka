package pl.edu.agh.backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;

/** CSRF runs before authentication, so these pin down which of 401/403 a write without a CSRF token gets. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AnonymousWriteTest {

    private static final String DEVICE_PATH = "/api/notifications/tokens/ExponentPushToken[anon-write-test]";
    private static final String DEVICE_BODY = """
            {"platform":"ANDROID"}
            """;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousWriteWithoutCsrfTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put(DEVICE_PATH).contentType(MediaType.APPLICATION_JSON).content(DEVICE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousWriteWithCsrfTokenIsUnauthorized() throws Exception {
        mockMvc.perform(put(DEVICE_PATH)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEVICE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionWriteWithoutCsrfTokenIsStillForbidden() throws Exception {
        mockMvc.perform(put(DEVICE_PATH)
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DEVICE_BODY))
                .andExpect(status().isForbidden());
    }
}
