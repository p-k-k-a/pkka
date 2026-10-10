package pl.edu.agh.backend.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest(properties = "app.web.cookie-domain=pkka.me")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class WebCookieDomainIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiResponsesSetCsrfCookieOnConfiguredDomain() throws Exception {
        List<String> setCookies = mockMvc.perform(get("/api/public/events"))
                .andReturn()
                .getResponse()
                .getHeaders("Set-Cookie");

        assertThat(setCookies)
                .filteredOn(cookie -> cookie.startsWith("XSRF-TOKEN="))
                .singleElement()
                .asString()
                .contains("Domain=pkka.me");
    }

    @Test
    void logoutExpiresCsrfCookieOnConfiguredDomainAndSessionCookieOnApiHost() throws Exception {
        List<String> setCookies =
                mockMvc.perform(get("/logout")).andReturn().getResponse().getHeaders("Set-Cookie");

        assertThat(setCookies)
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith("JSESSIONID=")
                        .doesNotContain("Domain=")
                        .contains("Max-Age=0"))
                .anySatisfy(cookie -> assertThat(cookie)
                        .startsWith("XSRF-TOKEN=")
                        .contains("Domain=pkka.me")
                        .contains("Max-Age=0"));
    }
}
