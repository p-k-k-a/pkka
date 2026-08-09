package pl.edu.agh.backend.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void listForAnonymousUserQueriesPublicAudienceOnly() {
        Event publicEvent = Event.builder()
                .title("Public")
                .type(EventType.ONLINE)
                .startsAt(Instant.now().plusSeconds(3600))
                .endsAt(Instant.now().plusSeconds(7200))
                .audience(Audience.PUBLIC)
                .build();
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(publicEvent)));

        var page = eventService.list(null, Set.of(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByIdHidesAlumniOnlyEventFromAnonymousUser() {
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

        assertThatThrownBy(() -> eventService.findById(id, null)).isInstanceOf(EventNotFoundException.class);
    }

    @Test
    void findByIdAllowsVerifiedAlumnToSeeAlumniOnlyEvent() {
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

        Jwt jwt =
                Jwt.withTokenValue("token").header("alg", "none").subject("sub").build();
        var auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VERIFIED_ALUMN")));

        Event found = eventService.findById(id, auth);

        assertThat(found.getAudience()).isEqualTo(Audience.ALL_ALUMNI);
    }
}
