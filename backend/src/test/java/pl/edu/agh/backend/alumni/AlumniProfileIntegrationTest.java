package pl.edu.agh.backend.alumni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.TagRepository;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService.UserIdentity;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor.UserPrincipalInfo;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(AlumniProfileIntegrationTest.TestSecurityBeans.class)
class AlumniProfileIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserProvisioningService userProvisioningService;

    @MockitoBean
    private KeycloakUserService keycloakUserService;

    private User alumn;

    @BeforeEach
    void setUp() {
        alumn = new User();
        alumn.setKeycloakId(UUID.randomUUID().toString());
        alumn.setFirstName("Jan");
        alumn.setLastName("Kowalski");
        alumn.setBio("Absolwent WI, backend developer.");
        alumn.setDiscordUsername("jan_kowalski");
        alumn.setCurrentPosition("Software Engineer");
        alumn.setCompany("ACME");
        alumn.setLinkedinUrl("https://linkedin.com/in/jankowalski");
        alumn.setGithubUrl("https://github.com/jankowalski");
        alumn.getTags().add(tagRepository.findAll().getFirst());
        alumn = userRepository.save(alumn);
    }

    @Test
    void returnsAlumniProfileForVerifiedAlumn() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", alumn.getId()).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alumn.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.bio").value("Absolwent WI, backend developer."))
                .andExpect(jsonPath("$.discordUsername").value("jan_kowalski"))
                .andExpect(jsonPath("$.currentPosition").value("Software Engineer"))
                .andExpect(jsonPath("$.company").value("ACME"))
                .andExpect(jsonPath("$.linkedinUrl").value("https://linkedin.com/in/jankowalski"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/jankowalski"))
                .andExpect(jsonPath("$.tags.length()").value(1));
    }

    @Test
    void returns404ForUnknownAlumniId() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", UUID.randomUUID()).with(verifiedAlumn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnverifiedUser() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", alumn.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatesOwnBioAndContactsViaProfileEndpoint() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Nowy opis",
                                  "discordUsername": "nowy_discord",
                                  "currentPosition": "Staff Engineer",
                                  "company": "ACME",
                                  "linkedinUrl": "https://linkedin.com/in/jankowalski",
                                  "githubUrl": "https://github.com/jankowalski"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Nowy opis"))
                .andExpect(jsonPath("$.discordUsername").value("nowy_discord"))
                .andExpect(jsonPath("$.firstName").value("Jan"));

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.getBio()).isEqualTo("Nowy opis");
        assertThat(updated.getDiscordUsername()).isEqualTo("nowy_discord");
    }

    @Test
    void rejectsTooLongBio() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"%s\"}".formatted("x".repeat(2001))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void provisioningStoresNamesFromKeycloak() {
        String keycloakId = UUID.randomUUID().toString();
        when(keycloakUserService.fetchIdentity(keycloakId)).thenReturn(Optional.of(new UserIdentity("Anna", "Nowak")));

        userProvisioningService.provisionIfAbsent(new UserPrincipalInfo(keycloakId));

        User provisioned = userRepository.findByKeycloakId(keycloakId).orElseThrow();
        assertThat(provisioned.getFirstName()).isEqualTo("Anna");
        assertThat(provisioned.getLastName()).isEqualTo("Nowak");
    }

    @Test
    void provisioningSyncsChangedNamesForExistingUser() {
        when(keycloakUserService.fetchIdentity(alumn.getKeycloakId()))
                .thenReturn(Optional.of(new UserIdentity("Janusz", null)));

        userProvisioningService.provisionIfAbsent(new UserPrincipalInfo(alumn.getKeycloakId()));

        User synced = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(synced.getFirstName()).isEqualTo("Janusz");
        assertThat(synced.getLastName()).isEqualTo("Kowalski");
    }

    @Test
    void provisioningKeepsStoredNamesWhenKeycloakUnavailable() {
        when(keycloakUserService.fetchIdentity(any())).thenReturn(Optional.empty());

        userProvisioningService.provisionIfAbsent(new UserPrincipalInfo(alumn.getKeycloakId()));

        User synced = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(synced.getFirstName()).isEqualTo("Jan");
        assertThat(synced.getLastName()).isEqualTo("Kowalski");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor verifiedAlumn() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
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
