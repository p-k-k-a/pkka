package pl.edu.agh.backend.alumni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import pl.edu.agh.backend.application.Application;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.application.Faculty;
import pl.edu.agh.backend.application.StudyType;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserPrincipalExtractor.UserPrincipalInfo;
import pl.edu.agh.backend.user.UserProvisioningService;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTagRepository;

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
    private UserTagRepository userTagRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

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
        alumn.setEmail("jan.kowalski@example.com");
        alumn.setBio("Absolwent WI, backend developer.");
        alumn.setDiscordId("123456789012345678");
        alumn.setCurrentPosition("Software Engineer");
        alumn.setCompany("ACME");
        alumn.setLinkedinUrl("https://linkedin.com/in/jankowalski");
        alumn.setGithubUrl("https://github.com/jankowalski");
        alumn.setWillingToMentor(true);
        alumn.getTags().add(userTagRepository.findAll().getFirst());
        alumn = userRepository.save(alumn);

        // Education facts come from the applicant's approved application.
        Application approved = Application.builder()
                .applicant(alumn)
                .status(ApplicationStatus.APPROVED)
                .faculty(Faculty.WI)
                .fieldOfStudy("Informatyka")
                .studyType(StudyType.MASTER)
                .graduationYear(2020)
                .phoneNumber("+48123456789")
                .reviewedAt(Instant.parse("2021-06-15T10:00:00Z"))
                .build();
        applicationRepository.save(approved);
    }

    @Test
    void returnsAlumniProfileForVerifiedAlumn() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", alumn.getId()).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alumn.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.bio").value("Absolwent WI, backend developer."))
                .andExpect(jsonPath("$.discordId").value("123456789012345678"))
                .andExpect(jsonPath("$.currentPosition").value("Software Engineer"))
                .andExpect(jsonPath("$.company").value("ACME"))
                .andExpect(jsonPath("$.linkedinUrl").value("https://linkedin.com/in/jankowalski"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/jankowalski"))
                .andExpect(jsonPath("$.graduationYear").value(2020))
                .andExpect(jsonPath("$.fieldOfStudy").value("Informatyka"))
                .andExpect(jsonPath("$.alumnSince").value(2021))
                .andExpect(jsonPath("$.willingToMentor").value(true))
                .andExpect(jsonPath("$.tags.length()").value(1))
                .andExpect(jsonPath("$.visibility.name").value(true))
                .andExpect(jsonPath("$.visibility.email").value(true))
                .andExpect(jsonPath("$.visibility.discord").value(true));
    }

    @Test
    void returns404ForUnknownAlumniId() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", UUID.randomUUID()).with(verifiedAlumn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns404ForUserWithoutApprovedApplication() throws Exception {
        // A bare user row with no approved application (e.g. still under review, or rejected) is not a
        // verified alumnus and must not be reachable via the public alumni profile endpoint either.
        User unverified = new User();
        unverified.setKeycloakId(UUID.randomUUID().toString());
        unverified = userRepository.save(unverified);

        mockMvc.perform(get("/api/alumni/{id}", unverified.getId()).with(verifiedAlumn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnverifiedUser() throws Exception {
        mockMvc.perform(get("/api/alumni/{id}", alumn.getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void hidesFieldsAccordingToVisibilitySettings() throws Exception {
        alumn.setShowName(false);
        alumn.setShowEmail(false);
        alumn.setShowDiscord(false);
        userRepository.save(alumn);

        mockMvc.perform(get("/api/alumni/{id}", alumn.getId()).with(verifiedAlumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").doesNotExist())
                .andExpect(jsonPath("$.lastName").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.discordId").doesNotExist())
                // non-hideable fields stay visible
                .andExpect(jsonPath("$.bio").value("Absolwent WI, backend developer."))
                .andExpect(jsonPath("$.company").value("ACME"))
                // visibility flags mirror the owner's settings
                .andExpect(jsonPath("$.visibility.name").value(false))
                .andExpect(jsonPath("$.visibility.email").value(false))
                .andExpect(jsonPath("$.visibility.discord").value(false));
    }

    @Test
    void updatesOwnProfileViaProfileEndpoint() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Nowy opis",
                                  "currentPosition": "Staff Engineer",
                                  "company": "ACME",
                                  "linkedinUrl": "https://linkedin.com/in/jankowalski",
                                  "githubUrl": "https://github.com/jankowalski"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Nowy opis"))
                .andExpect(jsonPath("$.currentPosition").value("Staff Engineer"))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                // omitted field: partial update must leave it unchanged (set to true in setUp)
                .andExpect(jsonPath("$.willingToMentor").value(true))
                // education facts surface on the own-profile view too
                .andExpect(jsonPath("$.graduationYear").value(2020))
                .andExpect(jsonPath("$.fieldOfStudy").value("Informatyka"))
                .andExpect(jsonPath("$.alumnSince").value(2021));

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.getBio()).isEqualTo("Nowy opis");
        assertThat(updated.getCurrentPosition()).isEqualTo("Staff Engineer");
    }

    @Test
    void updatesWillingToMentorViaProfileEndpoint() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"willingToMentor\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.willingToMentor").value(false));

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.isWillingToMentor()).isFalse();
    }

    @Test
    void partialUpdateKeepsOmittedFields() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\": \"Tylko opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Tylko opis"))
                // omitted fields must survive the partial update
                .andExpect(jsonPath("$.company").value("ACME"))
                .andExpect(jsonPath("$.currentPosition").value("Software Engineer"));

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.getCompany()).isEqualTo("ACME");
        assertThat(updated.getCurrentPosition()).isEqualTo("Software Engineer");
    }

    @Test
    void blankStringClearsField() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").doesNotExist());

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.getCompany()).isNull();
    }

    @Test
    void updatesVisibilityTogglesViaProfileEndpoint() throws Exception {
        mockMvc.perform(patch("/api/profiles/me")
                        .with(jwt().jwt(jwt -> jwt.subject(alumn.getKeycloakId()))
                                .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\": {\"email\": false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility.email").value(false))
                // untouched toggles keep their default
                .andExpect(jsonPath("$.visibility.name").value(true))
                .andExpect(jsonPath("$.visibility.discord").value(true));

        User updated = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(updated.isShowEmail()).isFalse();
        assertThat(updated.isShowName()).isTrue();
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
    void syncIdentityFromClaimsStoresNamesEmailAndFetchesDiscordId() {
        String keycloakId = UUID.randomUUID().toString();
        when(keycloakUserService.fetchDiscordId(keycloakId)).thenReturn(Optional.of("222333444555666777"));

        userProvisioningService.syncIdentityFromClaims(keycloakId, "Anna", "Nowak", "anna.nowak@example.com");

        User provisioned = userRepository.findByKeycloakId(keycloakId).orElseThrow();
        assertThat(provisioned.getFirstName()).isEqualTo("Anna");
        assertThat(provisioned.getLastName()).isEqualTo("Nowak");
        assertThat(provisioned.getEmail()).isEqualTo("anna.nowak@example.com");
        assertThat(provisioned.getDiscordId()).isEqualTo("222333444555666777");
    }

    @Test
    void syncIdentityFromClaimsUpdatesChangedNameAndSkipsDiscordWhenPresent() {
        // alumn already has a discordId, so the federated lookup must be skipped; null claims keep stored values
        userProvisioningService.syncIdentityFromClaims(alumn.getKeycloakId(), "Janusz", null, null);

        User synced = userRepository.findByKeycloakId(alumn.getKeycloakId()).orElseThrow();
        assertThat(synced.getFirstName()).isEqualTo("Janusz");
        assertThat(synced.getLastName()).isEqualTo("Kowalski");
        assertThat(synced.getEmail()).isEqualTo("jan.kowalski@example.com");
        verify(keycloakUserService, never()).fetchDiscordId(any());
    }

    @Test
    void provisionIfAbsentCreatesRowWithoutContactingKeycloak() {
        String keycloakId = UUID.randomUUID().toString();

        userProvisioningService.provisionIfAbsent(new UserPrincipalInfo(keycloakId));

        User created = userRepository.findByKeycloakId(keycloakId).orElseThrow();
        assertThat(created.getFirstName()).isNull();
        verify(keycloakUserService, never()).fetchDiscordId(any());
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
