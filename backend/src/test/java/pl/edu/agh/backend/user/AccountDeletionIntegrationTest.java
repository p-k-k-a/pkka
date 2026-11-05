package pl.edu.agh.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.edu.agh.backend.support.JwtTestSupport.asAdmin;
import static pl.edu.agh.backend.support.JwtTestSupport.asUser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.event.registration.RegistrationPromotedEvent;
import pl.edu.agh.backend.infrastructure.keycloak.KeycloakUserService;
import pl.edu.agh.backend.storage.StorageService;
import pl.edu.agh.backend.support.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@RecordApplicationEvents
@Import(TestSecurityConfig.class)
class AccountDeletionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private static final String EMAIL = "anna@example.com";
    private static final String AVATAR_KEY =
            "avatars/0f3a1c8e-1111-4000-8000-000000000001/0f3a1c8e-1111-4000-8000-000000000002.png";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private KeycloakUserService keycloakUserService;

    @MockitoBean
    private StorageService storageService;

    private User user;
    private User other;

    @BeforeEach
    void setUp() {
        user = newUser(EMAIL);
        user.setAvatarKey(AVATAR_KEY);
        user = userRepository.saveAndFlush(user);
        other = userRepository.saveAndFlush(newUser("other@example.com"));
    }

    @Test
    void deletesTheUserAndEverythingThatIsTheirs() throws Exception {
        UUID tag = UUID.randomUUID();
        jdbc.update("insert into user_skill_tags values (?, 'Account deletion')", tag);
        jdbc.update("insert into user_tags values (?, ?)", user.getId(), tag);
        UUID application = insertApplication(user, null);
        jdbc.update(
                "insert into application_consents values (?, ?, 'GDPR_DATA_PROCESSING', now())",
                UUID.randomUUID(),
                application);
        jdbc.update(
                "insert into tickets (id, category, title, description, author_id, created_at, updated_at)"
                        + " values (?, 'OTHER', 't', 'd', ?, now(), now())",
                UUID.randomUUID(),
                user.getId());
        UUID survey = UUID.randomUUID();
        jdbc.update(
                "insert into surveys (id, title, ends_at, created_at, updated_at) values (?, 's', now(), now(), now())",
                survey);
        jdbc.update("insert into survey_submissions values (?, ?, ?, now())", UUID.randomUUID(), survey, user.getId());
        jdbc.update("insert into device_tokens values ('token', ?, 'ANDROID', now(), now())", user.getId());
        jdbc.update("insert into push_tickets values ('ticket', 'token', now())");

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
        for (String table : new String[] {"user_tags", "survey_submissions", "device_tokens", "event_registrations"}) {
            assertThat(count("select count(*) from " + table + " where user_id = ?", user.getId()))
                    .as(table)
                    .isZero();
        }
        assertThat(count("select count(*) from applications where applicant_id = ?", user.getId()))
                .isZero();
        assertThat(count("select count(*) from tickets where author_id = ?", user.getId()))
                .isZero();
        assertThat(count("select count(*) from application_consents where application_id = ?", application))
                .isZero();
        assertThat(count("select count(*) from push_tickets where token = 'token'"))
                .isZero();
        verify(keycloakUserService).deleteUser(user.getKeycloakId());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        verify(storageService).delete(AVATAR_KEY);
    }

    @Test
    void keepsWhatTheUserDidAsAnAdminWithoutTheirName() throws Exception {
        UUID application = insertApplication(other, user);
        UUID ticket = UUID.randomUUID();
        jdbc.update(
                "insert into tickets (id, category, title, description, author_id, responded_by_id, created_at,"
                        + " updated_at) values (?, 'OTHER', 't', 'd', ?, ?, now(), now())",
                ticket,
                other.getId(),
                user.getId());
        UUID post = UUID.randomUUID();
        jdbc.update(
                "insert into posts (id, slug, title, content, author_id, created_at, updated_at)"
                        + " values (?, 'p', 'p', 'c', ?, now(), now())",
                post,
                user.getId());
        Event event = newEvent(Instant.now().plus(7, ChronoUnit.DAYS), null);
        event.setAuthor(user);
        eventRepository.saveAndFlush(event);

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(column("applications", "reviewed_by_id", application)).isNull();
        assertThat(column("tickets", "responded_by_id", ticket)).isNull();
        assertThat(column("posts", "author_id", post)).isNull();
        assertThat(column("events", "author_id", event.getId())).isNull();
    }

    @Test
    void givesTheFreedSeatToTheWaitlistAndKeepsTheStatisticsAnonymous() throws Exception {
        Event event = newEvent(Instant.now().plus(7, ChronoUnit.DAYS), 1);
        register(event, user, EventRegistrationStatus.REGISTERED);
        EventRegistration queued = register(event, other, EventRegistrationStatus.WAITLISTED);
        jdbc.update(
                "insert into event_registration_activities values (?, ?, ?, 'SIGNED_UP', now())",
                UUID.randomUUID(),
                event.getId(),
                user.getId());

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(eventRegistrationRepository.findById(queued.getId()))
                .get()
                .extracting(EventRegistration::getStatus)
                .isEqualTo(EventRegistrationStatus.REGISTERED);
        assertThat(applicationEvents.stream(RegistrationPromotedEvent.class))
                .containsExactly(new RegistrationPromotedEvent(event.getId(), other.getId()));
        assertThat(jdbc.queryForList(
                        "select type from event_registration_activities where event_id = ? and user_id is null",
                        String.class,
                        event.getId()))
                .containsExactlyInAnyOrder("SIGNED_UP", "CANCELLED");
    }

    @Test
    void keepsPastSignUpsForTheStatisticsWithoutThePerson() throws Exception {
        Event past = newEvent(Instant.now().minus(1, ChronoUnit.DAYS), null);
        register(past, user, EventRegistrationStatus.REGISTERED);

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(count(
                        "select count(*) from event_registrations where event_id = ? and user_id is null",
                        past.getId()))
                .isEqualTo(1);
        mockMvc.perform(get("/api/admin/events/{id}/registrations", past.getId())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].displayName").value(nullValue()))
                .andExpect(jsonPath("$[0].email").value(nullValue()));
    }

    @Test
    void leavingTheWaitlistPromotesNobody() throws Exception {
        Event event = newEvent(Instant.now().plus(7, ChronoUnit.DAYS), 1);
        register(event, other, EventRegistrationStatus.REGISTERED);
        register(event, user, EventRegistrationStatus.WAITLISTED);

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(applicationEvents.stream(RegistrationPromotedEvent.class)).isEmpty();
        assertThat(jdbc.queryForList(
                        "select type from event_registration_activities where event_id = ?",
                        String.class,
                        event.getId()))
                .containsExactly("LEFT_WAITLIST");
    }

    @Test
    void skipsRegistrationsForDeletedEvents() throws Exception {
        Event event = newEvent(Instant.now().plus(7, ChronoUnit.DAYS), 1);
        register(event, user, EventRegistrationStatus.REGISTERED);
        jdbc.update("update events set deleted_at = now() where id = ?", event.getId());

        deleteAccount(EMAIL).andExpect(status().isNoContent());

        assertThat(count("select count(*) from event_registrations where user_id = ?", user.getId()))
                .isZero();
    }

    @Test
    void acceptsTheEmailInAnyCaseAndWithSpaces() throws Exception {
        deleteAccount("  ANNA@example.com ").andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void refusesAWrongEmailAndDeletesNothing() throws Exception {
        deleteAccount("other@example.com").andExpect(status().isBadRequest());

        assertThat(userRepository.findById(user.getId())).isPresent();
        verify(keycloakUserService, never()).deleteUser(any());
    }

    @Test
    void refusesWhenKeycloakFails() throws Exception {
        doThrow(new IllegalStateException("down")).when(keycloakUserService).deleteUser(any());

        assertThatThrownBy(() -> deleteAccount(EMAIL)).hasRootCauseMessage("down");
        verify(storageService, never()).delete(any());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/profiles/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions deleteAccount(String email) throws Exception {
        return mockMvc.perform(delete("/api/profiles/me")
                .with(asUser(user.getKeycloakId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private static User newUser(String email) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        user.setEmail(email);
        return user;
    }

    private UUID insertApplication(User applicant, User reviewer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "insert into applications (id, applicant_id, status, discord_vouched, reviewed_by_id, created_at,"
                        + " updated_at) values (?, ?, 'APPROVED', true, ?, now(), now())",
                id,
                applicant.getId(),
                reviewer == null ? null : reviewer.getId());
        return id;
    }

    private Event newEvent(Instant startsAt, Integer seatLimit) {
        return eventRepository.saveAndFlush(Event.builder()
                .title("Event")
                .slug("event-" + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .seatLimit(seatLimit)
                .audience(Audience.PUBLIC)
                .build());
    }

    private EventRegistration register(Event event, User user, EventRegistrationStatus status) {
        return eventRegistrationRepository.saveAndFlush(EventRegistration.builder()
                .event(event)
                .user(user)
                .status(status)
                .build());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private Object column(String table, String column, UUID id) {
        return jdbc.queryForObject("select " + column + " from " + table + " where id = ?", Object.class, id);
    }
}
