package pl.edu.agh.backend.event;

import static pl.edu.agh.backend.event.EventSpecifications.*;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.dto.EventDetailsResponse;
import pl.edu.agh.backend.event.dto.EventListItemResponse;
import pl.edu.agh.backend.event.registration.EventRegistrationService;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.security.Caller;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventLookup eventLookup;
    private final EventRegistrationService eventRegistrationService;

    public Page<EventListItemResponse> list(
            Caller caller, Collection<String> tagNames, EventTimeframe timeframe, Pageable pageable) {
        Specification<Event> spec = Specification.allOf(
                inTimeframe(timeframe), audienceIn(EventVisibility.audiencesOf(caller)), hasAnyTag(tagNames));

        Page<Event> events = eventRepository.findAll(spec, pageable);
        List<UUID> ids = events.getContent().stream().map(Event::getId).toList();
        Map<UUID, Long> seatsTaken = eventRegistrationService.seatsTakenByEvent(ids);
        Map<UUID, EventRegistrationStatus> ownStatus = eventRegistrationService.ownStatusByEvent(caller, ids);

        return events.map(event -> EventListItemResponse.from(
                event, seatsTaken.getOrDefault(event.getId(), 0L), ownStatus.get(event.getId())));
    }

    public EventDetailsResponse getDetails(UUID id, Caller caller) {
        Event event = eventLookup.findVisible(id, caller);
        EventRegistrationStatus registrationStatus =
                eventRegistrationService.findOwnStatus(id, caller).orElse(null);
        return EventDetailsResponse.from(event, eventRegistrationService.seatsTaken(id), registrationStatus);
    }

    private Specification<Event> inTimeframe(EventTimeframe timeframe) {
        Instant now = Instant.now();
        return switch (timeframe == null ? EventTimeframe.UPCOMING : timeframe) {
            case UPCOMING -> startsAfter(now);
            case PAST -> startsBeforeOrEqual(now);
            case ALL -> Specification.unrestricted();
        };
    }
}
