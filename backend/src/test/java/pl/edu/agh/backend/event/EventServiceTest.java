package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.security.Caller;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventLookup eventLookup;

    @Mock
    private EventRegistrationService eventRegistrationService;

    @InjectMocks
    private EventService eventService;

    @Test
    void listForAnonymousUserQueriesPublicAudienceOnly() {
        UUID eventId = UUID.randomUUID();
        Event publicEvent = Event.builder()
                .id(eventId)
                .title("Public")
                .type(EventType.ONLINE)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.PUBLIC)
                .build();
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(publicEvent)));
        when(eventRegistrationService.seatsTakenByEvent(List.of(eventId))).thenReturn(Map.of(eventId, 3L));
        when(eventRegistrationService.ownStatusByEvent(Caller.anonymous(), List.of(eventId)))
                .thenReturn(Map.of());

        var page = eventService.list(Caller.anonymous(), Set.of(), EventTimeframe.UPCOMING, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().seatsTaken()).isEqualTo(3);
        assertThat(page.getContent().getFirst().registrationStatus()).isNull();
    }

    @Test
    void getDetailsReportsHeldSeatsAndTheCallersOwnStatus() {
        UUID id = UUID.randomUUID();
        Caller caller = new Caller("sub", Set.of());
        Event event = Event.builder()
                .id(id)
                .title("Networking")
                .type(EventType.IN_PERSON)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .seatLimit(5)
                .audience(Audience.PUBLIC)
                .build();
        when(eventLookup.findVisible(id, caller)).thenReturn(event);
        when(eventRegistrationService.seatsTaken(id)).thenReturn(5L);
        when(eventRegistrationService.findOwnStatus(id, caller))
                .thenReturn(Optional.of(EventRegistrationStatus.WAITLISTED));

        var details = eventService.getDetails(id, caller);

        assertThat(details.seatsTaken()).isEqualTo(5);
        assertThat(details.registrationStatus()).isEqualTo(EventRegistrationStatus.WAITLISTED);
    }
}
