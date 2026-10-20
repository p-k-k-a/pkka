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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.tag.Tag;
import pl.edu.agh.backend.event.tag.TagRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.security.Roles;
import pl.edu.agh.backend.storage.AdminUploadPurpose;
import pl.edu.agh.backend.storage.StorageService;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private EventRegistrationService eventRegistrationService;

    @Mock
    private CallerUserService callerUserService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private AdminEventService adminEventService;

    private static final Caller ADMIN = new Caller("admin-sub", Set.of(Roles.ADMIN));

    @Test
    void getThrowsWhenMissing() {
        when(eventRepository.findBySlug("brak")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminEventService.getBySlug("brak")).isInstanceOf(EventNotFoundException.class);
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
        when(eventRepository.findBySlug("warsztat-ai")).thenReturn(Optional.of(event));
        when(eventRegistrationService.seatsTaken(id)).thenReturn(7L);

        assertThat(adminEventService.getBySlug("warsztat-ai").seatsTaken()).isEqualTo(7);
    }

    @Test
    void createPersistsResolvedTagsAndStampsAuthor() {
        Tag ai = Tag.builder().name("ai").build();
        User author = new User();
        when(tagRepository.findByNameIn(Set.of("ai"))).thenReturn(Set.of(ai));
        when(callerUserService.getOrCreate(ADMIN)).thenReturn(author);
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.existsBySlug("warsztat-ai")).thenReturn(true);

        EventRequest request = new EventRequest(
                "Warsztat AI",
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
                null,
                Set.of("ai"));

        AdminEventResponse response = adminEventService.create(ADMIN, request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTags()).containsExactlyInAnyOrder(ai);
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
        assertThat(response.title()).isEqualTo("Warsztat AI");
        assertThat(response.slug()).isEqualTo("warsztat-ai-2");
        assertThat(response.tags()).containsExactlyInAnyOrder("ai");
        verify(eventPublisher).publishEvent(new EventCreatedEvent(response.id()));
    }

    @Test
    void createSkipsSlugsTakenByStaticPages() {
        when(callerUserService.getOrCreate(ADMIN)).thenReturn(new User());
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest request = new EventRequest(
                "Archive",
                "Opis",
                EventType.ONLINE,
                Instant.parse("2026-09-01T17:00:00Z"),
                Instant.parse("2026-09-01T19:00:00Z"),
                "https://meet.example.com",
                null,
                40,
                null,
                Audience.PUBLIC,
                null,
                null,
                Set.of());

        assertThat(adminEventService.create(ADMIN, request).slug()).isEqualTo("archive-2");
    }

    @Test
    void createStoresTheCoverAsAReferenceAndRespondsWithASignedUrl() {
        User author = new User();
        when(callerUserService.getOrCreate(ADMIN)).thenReturn(author);
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.toStoredReference(AdminUploadPurpose.EVENT_COVER, "covers/events/a.png"))
                .thenReturn("covers/events/a.png");
        when(storageService.resolveUrl("covers/events/a.png")).thenReturn("https://r2.example/signed");

        AdminEventResponse response = adminEventService.create(ADMIN, requestWithCover("covers/events/a.png"));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCoverImageUrl()).isEqualTo("covers/events/a.png");
        assertThat(response.coverImageUrl()).isEqualTo("https://r2.example/signed");
    }

    @Test
    void createRejectsACoverReferenceTooLongToStore() {
        String tooLong = "https://example.com/" + "a".repeat(600);
        when(storageService.toStoredReference(AdminUploadPurpose.EVENT_COVER, tooLong))
                .thenReturn(tooLong);

        assertThatThrownBy(() -> adminEventService.create(ADMIN, requestWithCover(tooLong)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static EventRequest requestWithCover(String coverImageUrl) {
        return new EventRequest(
                "Warsztat",
                null,
                EventType.ONLINE,
                Instant.parse("2026-09-01T17:00:00Z"),
                Instant.parse("2026-09-01T19:00:00Z"),
                null,
                null,
                null,
                null,
                Audience.PUBLIC,
                coverImageUrl,
                null,
                null);
    }

    @Test
    void createRejectsUnknownTags() {
        when(tagRepository.findByNameIn(Set.of("nope"))).thenReturn(Set.of());

        EventRequest request = new EventRequest(
                "Warsztat",
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
