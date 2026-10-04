package pl.edu.agh.backend.event.registration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventType;
import pl.edu.agh.backend.support.JwtTestSupport;
import pl.edu.agh.backend.support.TestSecurityConfig;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@Import(TestSecurityConfig.class)
class AdminEventRegistrationEndpointTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Event newEvent(Integer seatLimit) {
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        return eventRepository.saveAndFlush(Event.builder()
                .title("Wydarzenie " + UUID.randomUUID())
                .type(EventType.IN_PERSON)
                .startsAt(startsAt)
                .endsAt(startsAt.plus(2, ChronoUnit.HOURS))
                .seatLimit(seatLimit)
                .audience(Audience.PUBLIC)
                .build());
    }

    private String signUp(Event event, String firstName, String lastName, String email) throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/events/{id}/registration", event.getId())
                        .with(JwtTestSupport.asUser(keycloakId))
                        .with(csrf()))
                .andExpect(status().isCreated());
        User user = userRepository.findByKeycloakId(keycloakId).orElseThrow();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        userRepository.saveAndFlush(user);
        return keycloakId;
    }

    @Test
    void rejectsNonAdmins() throws Exception {
        Event event = newEvent(10);

        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asUser()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asVerifiedAlumn()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownOrDeletedEvent_isNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/events/{id}/registrations", UUID.randomUUID())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());

        Event deleted = newEvent(10);
        eventRepository.delete(deleted);
        eventRepository.flush();
        mockMvc.perform(get("/api/admin/events/{id}/registrations", deleted.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eventWithoutSignUps_listsNobody() throws Exception {
        Event event = newEvent(10);

        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listsSeatHoldersInSignUpOrderThenTheWaitlistInQueueOrder() throws Exception {
        Event event = newEvent(2);
        signUp(event, "Anna", "Kowalska", "anna@example.com");
        signUp(event, "Tomasz", "Nowak", "tomasz@example.com");
        signUp(event, "Maria", "Wójcik", "maria@example.com");
        signUp(event, "Paweł", "Zieliński", "pawel@example.com");

        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName")
                        .value(contains("Anna Kowalska", "Tomasz Nowak", "Maria Wójcik", "Paweł Zieliński")))
                .andExpect(
                        jsonPath("$[*].status").value(contains("REGISTERED", "REGISTERED", "WAITLISTED", "WAITLISTED")))
                .andExpect(jsonPath("$[0].email").value("anna@example.com"))
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].registeredAt").exists())
                .andExpect(jsonPath("$[0].waitlistPosition").doesNotExist())
                .andExpect(jsonPath("$[2].waitlistPosition").value(1))
                .andExpect(jsonPath("$[3].waitlistPosition").value(2));
    }

    @Test
    void cancellationsDropOutAndPromotionsMoveUp() throws Exception {
        Event event = newEvent(1);
        String first = signUp(event, "Anna", "Kowalska", "anna@example.com");
        signUp(event, "Tomasz", "Nowak", "tomasz@example.com");
        signUp(event, "Maria", "Wójcik", "maria@example.com");

        mockMvc.perform(delete("/api/events/{id}/registration", event.getId())
                        .with(JwtTestSupport.asUser(first))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[*].displayName").value(contains("Tomasz Nowak", "Maria Wójcik")))
                .andExpect(jsonPath("$[*].status").value(contains("REGISTERED", "WAITLISTED")))
                .andExpect(jsonPath("$[1].waitlistPosition").value(1));
    }

    @Test
    void userWithoutAProfileHasNoDisplayNameRatherThanTheWordNull() throws Exception {
        Event event = newEvent(10);
        signUp(event, null, null, null);
        signUp(event, "Anna", null, "anna@example.com");

        mockMvc.perform(get("/api/admin/events/{id}/registrations", event.getId())
                        .with(JwtTestSupport.asAdmin()))
                .andExpect(jsonPath("$[0].displayName").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[1].displayName").value("Anna"));
    }
}
