package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final String ROLE_VERIFIED_ALUMN = "ROLE_VERIFIED_ALUMN";

    private final EventRepository eventRepository;

    public Page<Event> list(Authentication authentication, Collection<String> tagNames, Pageable pageable) {
        Specification<Event> spec = Specification.where(startsAfter(Instant.now()))
                .and(audienceIn(visibleAudiences(authentication)))
                .and(hasAnyTag(tagNames));

        return eventRepository.findAll(spec, pageable);
    }

    public Event findById(UUID id, Authentication authentication) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
        if (!visibleAudiences(authentication).contains(event.getAudience())) {
            throw new EventNotFoundException(id);
        }
        return event;
    }

    private Set<Audience> visibleAudiences(Authentication authentication) {
        Set<Audience> audiences = EnumSet.of(Audience.PUBLIC);
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(ROLE_VERIFIED_ALUMN::equals)) {
            audiences.add(Audience.ALL_ALUMNI);
        }
        return audiences;
    }
}
