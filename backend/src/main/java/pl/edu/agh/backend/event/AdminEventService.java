package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.tag.Tag;
import pl.edu.agh.backend.event.tag.TagRepository;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final EventRegistrationService eventRegistrationService;
    private final CallerUserService callerUserService;
    private final ApplicationEventPublisher eventPublisher;

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
        User author = callerUserService.getOrCreate(caller);
        Event event = Event.builder()
                .author(author)
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
                .reminderLeadTimeMinutes(request.reminderLeadTimeMinutes())
                .tags(new HashSet<>(findOrCreateTags(() -> author, request.tags())))
                .build();

        Event created = eventRepository.saveAndFlush(event);
        eventPublisher.publishEvent(new EventCreatedEvent(created.getId()));
        return respond(created);
    }

    /**
     * Locks the event row like a sign-up does, because a raised seat limit hands seats to the waitlist and
     * that must not interleave with sign-ups and cancellations counting the same seats.
     */
    @Transactional
    public AdminEventResponse update(Caller caller, UUID id, EventRequest request) {
        Event event = eventRepository.findForUpdateById(id).orElseThrow(() -> new EventNotFoundException(id));
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
        event.setReminderLeadTimeMinutes(request.reminderLeadTimeMinutes());
        Set<Tag> tags = event.getTags();
        if (tags == null) {
            tags = new HashSet<>();
            event.setTags(tags);
        }
        tags.clear();
        tags.addAll(findOrCreateTags(() -> callerUserService.getOrCreate(caller), request.tags()));
        Event saved = eventRepository.saveAndFlush(event);
        dropUnusedTags();
        eventRegistrationService.fillFreeSeats(saved);
        return respond(saved);
    }

    @Transactional
    public void delete(UUID id) {
        eventRepository.delete(findOrThrow(id));
        dropUnusedTags();
    }

    private AdminEventResponse respond(Event event) {
        return AdminEventResponse.from(event, eventRegistrationService.seatsTaken(event.getId()));
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    private Set<Tag> findOrCreateTags(Supplier<User> creator, Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<Tag> existing = tagRepository.findForShareByNameIn(names);
        if (existing.size() == names.size()) {
            return existing;
        }
        Set<String> existingNames = existing.stream().map(Tag::getName).collect(Collectors.toSet());
        UUID creatorId = creator.get().getId();
        names.stream()
                .filter(name -> !existingNames.contains(name))
                .forEach(name -> tagRepository.insertIfAbsent(UUID.randomUUID(), name, creatorId));
        Set<Tag> resolved = tagRepository.findForShareByNameIn(names);
        if (resolved.size() != names.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tag names are not supported");
        }
        return resolved;
    }

    private void dropUnusedTags() {
        tagRepository.lockAdminCreated();
        tagRepository.deleteUnusedAdminCreated();
    }
}
