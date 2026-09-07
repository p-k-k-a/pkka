package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
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
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.EventSeatCount;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private CallerUserService callerUserService;

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
        when(eventRegistrationRepository.countByEventIdIn(List.of(eventId))).thenReturn(List.of(seatCount(eventId, 3)));

        var page = eventService.list(Caller.anonymous(), Set.of(), EventTimeframe.UPCOMING, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().seatsTaken()).isEqualTo(3);
        assertThat(page.getContent().getFirst().registered()).isFalse();
        // An anonymous caller has no row to look registrations up against, so none are fetched.
        verify(callerUserService).findId(Caller.anonymous());
    }

    @Test
    void findVisibleHidesAlumniOnlyEventFromAnonymousUser() {
        UUID id = UUID.randomUUID();
        Event alumniEvent = Event.builder()
                .id(id)
                .title("Alumni only")
                .type(EventType.IN_PERSON)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.ALL_ALUMNI)
                .build();
        when(eventRepository.findById(id)).thenReturn(java.util.Optional.of(alumniEvent));

        assertThatThrownBy(() -> eventService.findVisible(id, Caller.anonymous()))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void findVisibleAllowsVerifiedAlumnToSeeAlumniOnlyEvent() {
        UUID id = UUID.randomUUID();
        Event alumniEvent = Event.builder()
                .id(id)
                .title("Alumni only")
                .type(EventType.IN_PERSON)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.ALL_ALUMNI)
                .build();
        when(eventRepository.findById(id)).thenReturn(java.util.Optional.of(alumniEvent));

        Caller alumn = new Caller("sub", Set.of(Roles.VERIFIED_ALUMN));

        Event found = eventService.findVisible(id, alumn);

        assertThat(found.getAudience()).isEqualTo(Audience.ALL_ALUMNI);
    }

    private static EventSeatCount seatCount(UUID eventId, long seatsTaken) {
        return new EventSeatCount() {
            @Override
            public UUID getEventId() {
                return eventId;
            }

            @Override
            public long getSeatsTaken() {
                return seatsTaken;
            }
        };
    }
}
