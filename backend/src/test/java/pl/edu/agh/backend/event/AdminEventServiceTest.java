package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.tag.Tag;
import pl.edu.agh.backend.event.tag.TagRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private CallerUserService callerUserService;

    @InjectMocks
    private AdminEventService adminEventService;

    private static final Caller ADMIN = new Caller("admin-sub", Set.of(Roles.ADMIN));

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.get(id)).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void getReportsSeatsTaken() {
        UUID id = UUID.randomUUID();
        Event event = Event.builder()
                .id(id)
                .title("Warsztat AI")
                .type(EventType.ONLINE)
                .startsAt(Instant.parse("2026-09-01T17:00:00Z"))
                .endsAt(Instant.parse("2026-09-01T19:00:00Z"))
                .audience(Audience.PUBLIC)
                .tags(new HashSet<>())
                .build();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.countByEventId(id)).thenReturn(7L);

        assertThat(adminEventService.get(id).seatsTaken()).isEqualTo(7);
    }

    @Test
    void createPersistsResolvedTagsAndStampsAuthor() {
        Tag ai = Tag.builder().name("ai").build();
        User author = new User();
        when(tagRepository.findByNameIn(Set.of("ai"))).thenReturn(Set.of(ai));
        when(callerUserService.getOrCreate(ADMIN)).thenReturn(author);
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest request = new EventRequest(
                "Warsztat AI",
                "Krótki opis",
                "Opis",
                EventType.ONLINE,
                Instant.parse("2026-09-01T17:00:00Z"),
                Instant.parse("2026-09-01T19:00:00Z"),
                "https://meet.example.com",
                null,
                40,
                Instant.parse("2026-08-31T22:00:00Z"),
                Audience.PUBLIC,
                null,
                Set.of("ai"));

        AdminEventResponse response = adminEventService.create(ADMIN, request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTags()).containsExactlyInAnyOrder(ai);
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
        assertThat(response.title()).isEqualTo("Warsztat AI");
        assertThat(response.tags()).containsExactlyInAnyOrder("ai");
    }

    @Test
    void createRejectsUnknownTags() {
        when(tagRepository.findByNameIn(Set.of("nope"))).thenReturn(Set.of());

        EventRequest request = new EventRequest(
                "Warsztat",
                null,
                null,
                EventType.ONLINE,
                Instant.parse("2026-09-01T17:00:00Z"),
                Instant.parse("2026-09-01T19:00:00Z"),
                null,
                null,
                null,
                null,
                Audience.PUBLIC,
                null,
                Set.of("nope"));

        assertThatThrownBy(() -> adminEventService.create(ADMIN, request)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void listUpcomingDelegatesSpecification() {
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminEventService.list(EventTimeframe.UPCOMING, PageRequest.of(0, 10));

        verify(eventRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void deleteRemovesExistingEvent() {
        UUID id = UUID.randomUUID();
        Event event = Event.builder()
                .id(id)
                .title("Do usunięcia")
                .type(EventType.ONLINE)
                .startsAt(Instant.parse("2026-09-01T17:00:00Z"))
                .endsAt(Instant.parse("2026-09-01T19:00:00Z"))
                .audience(Audience.PUBLIC)
                .tags(new HashSet<>())
                .build();
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        adminEventService.delete(id);

        verify(eventRepository).delete(event);
    }
}
