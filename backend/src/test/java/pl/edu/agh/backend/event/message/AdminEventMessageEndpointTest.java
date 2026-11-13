package pl.edu.agh.backend.event.message;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

/** Rolled back after each test, so the after-commit delivery never runs; EventMailTest covers delivery. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminEventMessageEndpointTest {

    private static final String MESSAGE = """
            {"subject": "Zmiana sali", "body": "Spotykamy się w sali 2.41."}
            """;

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

    private Event newEvent() {
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        return eventRepository.saveAndFlush(Event.builder()
                .title("Wydarzenie " + UUID.randomUUID())
                .slug("wydarzenie-" + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .seatLimit(1)
                .audience(Audience.PUBLIC)
                .build());
    }

    private void signUp(Event event, EventRegistrationStatus status) {
        User user = new User();
        user.setKeycloakId(UUID.randomUUID().toString());
        eventRegistrationRepository.saveAndFlush(EventRegistration.builder()
                .event(event)
                .user(userRepository.save(user))
                .status(status)
                .build());
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        Event event = newEvent();

        mockMvc.perform(get("/api/admin/events/{id}/messages/recipients", event.getId())
                        .with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/events/{id}/messages", event.getId())
                        .with(JwtTestSupport.asUser())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MESSAGE))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownOrDeletedEvent_isNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/events/{id}/messages/recipients", UUID.randomUUID())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());

        Event deleted = newEvent();
        eventRepository.delete(deleted);
        eventRepository.flush();
        mockMvc.perform(post("/api/admin/events/{id}/messages", deleted.getId())
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MESSAGE))
                .andExpect(status().isNotFound());
    }

    @Test
    void countsTheWaitlistAmongRecipients() throws Exception {
        Event event = newEvent();
        signUp(event, EventRegistrationStatus.REGISTERED);
        signUp(event, EventRegistrationStatus.WAITLISTED);

        mockMvc.perform(get("/api/admin/events/{id}/messages/recipients", event.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients").value(2));
    }

    @Test
    void sendingAMessage_isAcceptedForEveryoneSignedUp() throws Exception {
        Event event = newEvent();
        signUp(event, EventRegistrationStatus.REGISTERED);

        mockMvc.perform(post("/api/admin/events/{id}/messages", event.getId())
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MESSAGE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.recipients").value(1));
    }

    @Test
    void aBlankSubjectOrBody_isRejected() throws Exception {
        Event event = newEvent();

        mockMvc.perform(post("/api/admin/events/{id}/messages", event.getId())
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": " ", "body": "Treść"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/events/{id}/messages", event.getId())
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": "Temat"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aSubjectOnMoreThanOneLine_isRejected() throws Exception {
        Event event = newEvent();

        mockMvc.perform(post("/api/admin/events/{id}/messages", event.getId())
                        .with(JwtTestSupport.asAdmin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": "Temat\\r\\nBcc: ola@example.com", "body": "Treść"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
