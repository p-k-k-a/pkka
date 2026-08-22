package pl.edu.agh.backend.security.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class MeControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void meReturnsJwtClaimsAndStrippedRoles() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-subject-42")
                .claim("email", "user@example.com")
                .claim("given_name", "Jan")
                .claim("family_name", "Kowalski")
                .claim("preferred_username", "jkowalski")
                .build();

        mockMvc.perform(get("/api/me")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt)
                                .authorities(
                                        new SimpleGrantedAuthority("ROLE_USER"),
                                        new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sub").value("kc-subject-42"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.preferredUsername").value("jkowalski"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "VERIFIED_ALUMN")));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}
