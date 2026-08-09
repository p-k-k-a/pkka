package pl.edu.agh.backend.security.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthRefreshControllerTest {

    private HttpServer server;
    private MockMvc mockMvc;
    private String tokenEndpoint;
    private String logoutEndpoint;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        int port = server.getAddress().getPort();
        tokenEndpoint = "http://127.0.0.1:" + port + "/token";
        logoutEndpoint = "http://127.0.0.1:" + port + "/logout";
        server.start();

        ClientRegistrationRepository repo = mock(ClientRegistrationRepository.class);
        when(repo.findByRegistrationId("keycloak-mobile")).thenReturn(mobileRegistration());

        mockMvc =
                MockMvcBuilders.standaloneSetup(new AuthRefreshController(repo)).build();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void refreshReturnsNewTokensOnSuccess() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] body = "{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        mockMvc.perform(post("/api/public/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"old-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access"))
                .andExpect(jsonPath("$.refresh_token").value("new-refresh"));
    }

    @Test
    void refreshReturns401OnInvalidGrant() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] body = "{\"error\":\"invalid_grant\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        mockMvc.perform(post("/api/public/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"expired\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReturns502OnUpstreamError() throws Exception {
        server.createContext("/token", exchange -> {
            byte[] body = "{\"error\":\"invalid_client\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        mockMvc.perform(post("/api/public/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"token\"}"))
                .andExpect(status().is(502));
    }

    @Test
    void refreshReturns400WhenTokenMissing() throws Exception {
        mockMvc.perform(post("/api/public/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutReturns204EvenWhenUpstreamReturns4xx() throws Exception {
        server.createContext("/logout", exchange -> exchange.sendResponseHeaders(400, -1));

        mockMvc.perform(post("/api/public/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"token\"}"))
                .andExpect(status().isNoContent());
    }

    private ClientRegistration mobileRegistration() {
        return ClientRegistration.withRegistrationId("keycloak-mobile")
                .clientId("pkka-mobile")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .issuerUri("http://127.0.0.1:8081/realms/pkka")
                .authorizationUri("http://127.0.0.1:8081/auth")
                .tokenUri(tokenEndpoint)
                .userInfoUri("http://127.0.0.1:8081/userinfo")
                .jwkSetUri("http://127.0.0.1:8081/jwks")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", logoutEndpoint))
                .build();
    }
}
