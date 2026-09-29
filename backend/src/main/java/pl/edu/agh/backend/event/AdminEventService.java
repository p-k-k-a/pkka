package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.tag.Tag;
import pl.edu.agh.backend.event.tag.TagRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final CallerUserService callerUserService;

    @Transactional(readOnly = true)
    public Page<AdminEventSummaryResponse> list(EventTimeframe timeframe, Pageable pageable) {
        Specification<Event> spec = Specification.unrestricted();
        Instant now = Instant.now();
        if (timeframe == EventTimeframe.UPCOMING) {
            spec = spec.and(startsAfter(now));
        } else if (timeframe == EventTimeframe.PAST) {
            spec = spec.and(startsBeforeOrEqual(now));
        }
        return eventRepository.findAll(spec, pageable).map(AdminEventSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminEventResponse get(UUID id) {
        return respond(findOrThrow(id));
    }

    @Transactional
    public AdminEventResponse create(Caller caller, EventRequest request) {
        Event event = Event.builder()
                .author(callerUserService.getOrCreate(caller))
                .title(request.title())
                .fullDescription(request.fullDescription())
                .type(request.type())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .transmissionUrl(request.transmissionUrl())
                .location(request.location())
                .seatLimit(request.seatLimit())
                .registrationClosesAt(request.registrationClosesAt())
                .audience(request.audience())
                .coverImageUrl(request.coverImageUrl())
                .tags(new HashSet<>(resolveTags(request.tags())))
                .build();
        return respond(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public AdminEventResponse update(UUID id, EventRequest request) {
        Event event = findOrThrow(id);
        event.setTitle(request.title());
        event.setFullDescription(request.fullDescription());
        event.setType(request.type());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setTransmissionUrl(request.transmissionUrl());
        event.setLocation(request.location());
        event.setSeatLimit(request.seatLimit());
        event.setRegistrationClosesAt(request.registrationClosesAt());
        event.setAudience(request.audience());
        event.setCoverImageUrl(request.coverImageUrl());
        Set<Tag> tags = event.getTags();
        if (tags == null) {
            tags = new HashSet<>();
            event.setTags(tags);
        }
        tags.clear();
        tags.addAll(resolveTags(request.tags()));
        return respond(eventRepository.saveAndFlush(event));
    }

    @Transactional
    public void delete(UUID id) {
        eventRepository.delete(findOrThrow(id));
    }

    private AdminEventResponse respond(Event event) {
        return AdminEventResponse.from(event, eventRegistrationRepository.countByEventId(event.getId()));
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    private Set<Tag> resolveTags(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<Tag> found = tagRepository.findByNameIn(names);
        if (found.size() != names.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tags are invalid");
        }
        return found;
    }
}
