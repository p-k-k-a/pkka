package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import pl.edu.agh.backend.infrastructure.slug.SlugUtils;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.storage.AdminUploadPurpose;
import pl.edu.agh.backend.storage.StorageService;
import pl.edu.agh.backend.user.CallerUserService;

@Service
@RequiredArgsConstructor
public class AdminEventService {

    // Static pages that sit next to /events/{slug}, so a slug must never take these names.
    private static final Set<String> ROUTE_SEGMENTS = Set.of("archive", "new");

    /** Length of the {@code cover_image_url} column. */
    private static final int MAX_STORED_COVER_LENGTH = 500;

    private final EventRepository eventRepository;
    private final TagRepository tagRepository;
    private final EventRegistrationService eventRegistrationService;
    private final CallerUserService callerUserService;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public Page<AdminEventSummaryResponse> list(EventTimeframe timeframe, Pageable pageable) {
        Specification<Event> spec = Specification.unrestricted();
        Instant now = Instant.now();
        if (timeframe == EventTimeframe.UPCOMING) {
            spec = spec.and(startsAfter(now));
        } else if (timeframe == EventTimeframe.PAST) {
            spec = spec.and(startsBeforeOrEqual(now));
        }
        return eventRepository
                .findAll(spec, pageable)
                .map(event ->
                        AdminEventSummaryResponse.from(event, storageService.resolveUrl(event.getCoverImageUrl())));
    }

    @Transactional(readOnly = true)
    public AdminEventResponse getBySlug(String slug) {
        return respond(eventRepository.findBySlug(slug).orElseThrow(() -> new EventNotFoundException(slug)));
    }

    @Transactional
    public AdminEventResponse create(Caller caller, EventRequest request) {
        Event event = Event.builder()
                .author(callerUserService.getOrCreate(caller))
                .slug(SlugUtils.uniqueSlug(
                        request.title(), slug -> ROUTE_SEGMENTS.contains(slug) || eventRepository.existsBySlug(slug)))
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
                .coverImageUrl(coverImageReference(request))
                .reminderLeadTimeMinutes(request.reminderLeadTimeMinutes())
                .tags(new HashSet<>(findOrCreateTags(request.tags())))
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
    public AdminEventResponse update(UUID id, EventRequest request) {
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
        event.setCoverImageUrl(coverImageReference(request));
        event.setReminderLeadTimeMinutes(request.reminderLeadTimeMinutes());
        Set<Tag> tags = event.getTags();
        if (tags == null) {
            tags = new HashSet<>();
            event.setTags(tags);
        }
        tags.clear();
        tags.addAll(findOrCreateTags(request.tags()));
        Event saved = eventRepository.saveAndFlush(event);
        eventRegistrationService.fillFreeSeats(saved);
        eventPublisher.publishEvent(new EventUpdatedEvent(saved.getId()));
        return respond(saved);
    }

    @Transactional
    public void delete(UUID id) {
        eventRepository.delete(findOrThrow(id));
        eventPublisher.publishEvent(new EventDeletedEvent(id));
    }

    private AdminEventResponse respond(Event event) {
        return AdminEventResponse.from(
                event,
                eventRegistrationService.seatsTaken(event.getId()),
                storageService.resolveUrl(event.getCoverImageUrl()));
    }

    /** External URL as is, or the key of an uploaded cover; a signed URL echoed back from a read is reduced to its key. */
    private String coverImageReference(EventRequest request) {
        String reference = storageService.toStoredReference(AdminUploadPurpose.EVENT_COVER, request.coverImageUrl());
        if (reference != null && reference.length() > MAX_STORED_COVER_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "coverImageUrl is too long");
        }
        return reference;
    }

    private Event findOrThrow(UUID id) {
        return eventRepository.findById(id).orElseThrow(() -> new EventNotFoundException(id));
    }

    private Set<Tag> findOrCreateTags(Set<String> names) {
        if (names.isEmpty()) {
            return Set.of();
        }
        names.forEach(name -> tagRepository.insertIfAbsent(UUID.randomUUID(), name));
        return tagRepository.findByNameIn(names);
    }
}
