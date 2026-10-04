package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;

@ExtendWith(MockitoExtension.class)
class EventLookupTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventLookup eventLookup;

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

        assertThatThrownBy(() -> eventLookup.findVisible(id, Caller.anonymous()))
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

        Event found = eventLookup.findVisible(id, alumn);

        assertThat(found.getAudience()).isEqualTo(Audience.ALL_ALUMNI);
    }
}
