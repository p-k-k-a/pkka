package pl.edu.agh.backend.event.registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.event.tag.Tag;
import pl.edu.agh.backend.event.tag.TagRepository;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** The "cannot be oversold" guarantee needs real parallel transactions and lives in {@link EventRegistrationConcurrencyTest}. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(EventRegistrationEndpointTest.TestSecurityBeans.class)
@RecordApplicationEvents
class EventRegistrationEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    private String alumnKeycloakId;

    @BeforeEach
    void setUp() {
        alumnKeycloakId = UUID.randomUUID().toString();
    }

    private Event newEvent(Audience audience, Integer seatLimit) {
        return newEvent(audience, seatLimit, Instant.now().plus(7, ChronoUnit.DAYS), null);
    }

    private Event newEvent(Audience audience, Integer seatLimit, Instant startsAt, Instant registrationClosesAt) {
        return eventRepository.save(Event.builder()
                .title("Test event " + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .seatLimit(seatLimit)
                .registrationClosesAt(registrationClosesAt)
                .audience(audience)
                .build());
    }

    /** The caller's own seat, inserted directly: by the time these tests run, POST would be rejected. */
    private void registerCaller(Event event) {
        User caller = new User();
        caller.setKeycloakId(alumnKeycloakId);
        caller = userRepository.save(caller);
        eventRegistrationRepository.save(EventRegistration.builder()
                .event(event)
                .user(caller)
                .status(EventRegistrationStatus.REGISTERED)
                .build());
    }

    private void registerOtherAlumn(Event event) {
        User other = new User();
        other.setKeycloakId(UUID.randomUUID().toString());
        other = userRepository.save(other);
        eventRegistrationRepository.save(EventRegistration.builder()
                .event(event)
                .user(other)
                .status(EventRegistrationStatus.REGISTERED)
                .build());
    }

    private RequestPostProcessor alumn() {
        return alumn(alumnKeycloakId);
    }

    /** Both roles, the way Keycloak issues them — {@code verified-alumn} comes on top of the default one. */
    private RequestPostProcessor alumn(String keycloakId) {
        return jwt().jwt(token -> token.subject(keycloakId))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN"));
    }

    private RequestPostProcessor plainUser() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Test
    void registerWithoutAuthentication_isUnauthorized() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerForPublicEventAsPlainUser_isAllowed() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(plainUser())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void registerForAlumniOnlyEventAsPlainUser_isNotFound() throws Exception {
        Event event = newEvent(Audience.ALL_ALUMNI, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(plainUser())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerForAlumniOnlyEventAsVerifiedAlumn_isAllowed() throws Exception {
        Event event = newEvent(Audience.ALL_ALUMNI, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void register_takesASeatAndReportsTheUpdatedCount() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(event.getId().toString()))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.waitlistPosition").doesNotExist())
                .andExpect(jsonPath("$.seatsTaken").value(2))
                .andExpect(jsonPath("$.seatLimit").value(10))
                .andExpect(jsonPath("$.registeredAt").exists());

        mockMvc.perform(get("/api/public/events/{id}", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsTaken").value(2))
                .andExpect(jsonPath("$.registrationStatus").value("REGISTERED"));
    }

    @Test
    void registerForEventWithoutSeatLimit_isAllowed() throws Exception {
        Event event = newEvent(Audience.PUBLIC, null);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatsTaken").value(1))
                .andExpect(jsonPath("$.seatLimit").value(nullValue()));
    }

    @Test
    void registerTwice_isRejectedAsAlreadyRegistered() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("ALREADY_REGISTERED"));
    }

    @Test
    void registerForFullEvent_joinsTheWaitlist() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(1))
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void registerBehindSomeoneAlreadyQueuing_reportsTheNextPlaceInLine() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn(UUID.randomUUID().toString()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlistPosition").value(1));

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlistPosition").value(2));
    }

    @Test
    void registerAfterTheDeadline_isRejectedAsClosed() throws Exception {
        Event event = newEvent(
                Audience.PUBLIC,
                10,
                Instant.now().plus(7, ChronoUnit.DAYS),
                Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("REGISTRATION_CLOSED"));
    }

    @Test
    void registerForAnEventThatHasAlreadyStarted_isRejectedAsClosed() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10, Instant.now().minus(1, ChronoUnit.HOURS), null);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("REGISTRATION_CLOSED"));
    }

    @Test
    void registerForUnknownEvent_isNotFound() throws Exception {
        mockMvc.perform(post("/api/events/{id}/registration", UUID.randomUUID())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerForEventOutsideOwnAudience_isNotFound() throws Exception {
        Event event = newEvent(Audience.SPECIFIC_GROUP, 10);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unregister_promotesTheFirstPersonQueuing() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REGISTERED"));

        String queuedKeycloakId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn(queuedKeycloakId))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(statusOf(event, queuedKeycloakId)).isEqualTo(EventRegistrationStatus.REGISTERED);
        mockMvc.perform(get("/api/public/events/{id}", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void unregister_announcesThePromotion() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerCaller(event);

        String queuedKeycloakId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn(queuedKeycloakId))
                        .with(csrf()))
                .andExpect(status().isCreated());
        UUID queuedUserId =
                userRepository.findByKeycloakId(queuedKeycloakId).orElseThrow().getId();

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(applicationEvents.stream(RegistrationPromotedEvent.class))
                .containsExactly(new RegistrationPromotedEvent(event.getId(), queuedUserId));
    }

    @Test
    void unregisterWhileOnlyQueuing_promotesNobody() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlistPosition").value(1));

        String behindKeycloakId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn(behindKeycloakId))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.waitlistPosition").value(2));

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(statusOf(event, behindKeycloakId)).isEqualTo(EventRegistrationStatus.WAITLISTED);
        mockMvc.perform(get("/api/public/events/{id}", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    private EventRegistrationStatus statusOf(Event event, String keycloakId) {
        UUID userId = userRepository.findByKeycloakId(keycloakId).orElseThrow().getId();
        return eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), userId)
                .orElseThrow()
                .getStatus();
    }

    @Test
    void unregisterAfterTheDeadlineButBeforeTheStart_isAllowed() throws Exception {
        Event event = newEvent(
                Audience.PUBLIC,
                10,
                Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now().minus(1, ChronoUnit.HOURS));
        registerCaller(event);

        // Nobody can claim the freed seat any more, but the organiser's head count stays honest.
        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unregisterAfterTheEventHasStarted_isRejected() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10, Instant.now().minus(1, ChronoUnit.HOURS), null);
        registerCaller(event);

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.reason").value("EVENT_ALREADY_STARTED"));
    }

    @Test
    void unregisterWithoutRegistration_isNotFound() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownRegistration_reportsAHeldSeat() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);
        registerCaller(event);

        mockMvc.perform(get("/api/events/{id}/registration", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.waitlistPosition").doesNotExist())
                .andExpect(jsonPath("$.seatsTaken").value(1))
                .andExpect(jsonPath("$.seatLimit").value(10));
    }

    @Test
    void ownRegistration_reportsHowFarDownTheQueueTheCallerIs() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn(UUID.randomUUID().toString()))
                        .with(csrf()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/events/{id}/registration", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andExpect(jsonPath("$.waitlistPosition").value(2))
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void ownRegistrationWithoutSigningUp_isNotFound() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);

        mockMvc.perform(get("/api/events/{id}/registration", event.getId()).with(alumn()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownRegistration_doesNotCreateAUserRowForACallerWithoutOne() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);
        String strangerKeycloakId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/events/{id}/registration", event.getId()).with(alumn(strangerKeycloakId)))
                .andExpect(status().isNotFound());

        assertThat(userRepository.findByKeycloakId(strangerKeycloakId)).isEmpty();
    }

    @Test
    void eventDetails_tellAQueuedSignUpApartFromAHeldSeat() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 1);
        registerOtherAlumn(event);

        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/public/events/{id}", event.getId()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("WAITLISTED"))
                .andExpect(jsonPath("$.seatsTaken").value(1));
    }

    @Test
    void eventList_reportsSeatsTakenPerEvent() throws Exception {
        // Scoped by a tag unique to this test — the dev seed events are loaded here too.
        Tag tag = tagRepository.save(Tag.builder()
                .name("seats-" + UUID.randomUUID().toString().substring(0, 8))
                .build());
        Event event = newEvent(Audience.PUBLIC, 10);
        event.getTags().add(tag);
        eventRepository.save(event);
        registerOtherAlumn(event);
        registerOtherAlumn(event);

        mockMvc.perform(get("/api/public/events").param("tags", tag.getName()).with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(event.getId().toString()))
                .andExpect(jsonPath("$.content[0].seatsTaken").value(2))
                .andExpect(jsonPath("$.content[0].seatLimit").value(10));
    }

    @Test
    void eventList_flagsTheCallersOwnRegistrations() throws Exception {
        Tag tag = tagRepository.save(Tag.builder()
                .name("mine-" + UUID.randomUUID().toString().substring(0, 8))
                .build());
        Event signedUpFor = newEvent(Audience.PUBLIC, 10);
        Event notSignedUpFor = newEvent(Audience.PUBLIC, 10);
        for (Event event : new Event[] {signedUpFor, notSignedUpFor}) {
            event.getTags().add(tag);
            eventRepository.save(event);
        }
        // Someone else's seat on the second event, so a non-zero count cannot be mistaken for the flag.
        registerOtherAlumn(notSignedUpFor);

        mockMvc.perform(post("/api/events/{id}/registration", signedUpFor.getId())
                        .with(alumn())
                        .with(csrf()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/public/events")
                        .param("tags", tag.getName())
                        .param("sort", "startsAt,asc")
                        .with(alumn()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id=='%s')].registrationStatus".formatted(signedUpFor.getId()))
                        .value(contains("REGISTERED")))
                .andExpect(jsonPath("$.content[?(@.id=='%s')].registrationStatus".formatted(notSignedUpFor.getId()))
                        .value(contains(nullValue())));
    }

    @Test
    void eventListForAnonymousCaller_reportsNothingAsRegistered() throws Exception {
        Tag tag = tagRepository.save(Tag.builder()
                .name("anon-" + UUID.randomUUID().toString().substring(0, 8))
                .build());
        Event event = newEvent(Audience.PUBLIC, 10);
        event.getTags().add(tag);
        eventRepository.save(event);
        registerOtherAlumn(event);

        mockMvc.perform(get("/api/public/events").param("tags", tag.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].seatsTaken").value(1))
                .andExpect(jsonPath("$.content[0].registrationStatus").doesNotExist());
    }

    @Test
    void eventDetailsForAnonymousCaller_reportsSeatsTakenButNoRegistration() throws Exception {
        Event event = newEvent(Audience.PUBLIC, 10);
        registerOtherAlumn(event);

        mockMvc.perform(get("/api/public/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatsTaken").value(1))
                .andExpect(jsonPath("$.registrationStatus").doesNotExist());
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
