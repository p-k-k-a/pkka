package pl.edu.agh.backend.event.statistics;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.EventSpecifications;
import pl.edu.agh.backend.event.EventTimeframe;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.EventSeatCount;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository.ActivityCount;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository.ActivityOccurrence;
import pl.edu.agh.backend.event.registration.RegistrationActivityType;

@Service
@RequiredArgsConstructor
public class EventStatisticsService {

    static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RegistrationActivityRepository registrationActivityRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public EventStatisticsResponse getStatistics(UUID eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        List<ActivityOccurrence> occurrences = registrationActivityRepository.findOccurrences(eventId);
        Map<RegistrationActivityType, Long> byType = new EnumMap<>(RegistrationActivityType.class);
        byType.putAll(occurrences.stream().collect(groupingBy(ActivityOccurrence::type, counting())));

        long registered =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
        long waitlisted =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.WAITLISTED);
        long leftWaitlist = count(byType, RegistrationActivityType.LEFT_WAITLIST);

        return new EventStatisticsResponse(
                event.getId(),
                event.getTitle(),
                event.getStartsAt(),
                event.getCreatedAt(),
                event.getSeatLimit(),
                (int) registered,
                (int) waitlisted,
                (int) (count(byType, RegistrationActivityType.SIGNED_UP)
                        + count(byType, RegistrationActivityType.WAITLISTED)),
                (int) (count(byType, RegistrationActivityType.CANCELLED) + leftWaitlist),
                (int) leftWaitlist,
                (int) count(byType, RegistrationActivityType.PROMOTED),
                occupancyPercent(registered, event.getSeatLimit()),
                daily(event, occurrences));
    }

    @Transactional(readOnly = true)
    public List<EventOccupancyResponse> compareOccupancy(EventTimeframe timeframe) {
        Instant now = Instant.now(clock);
        Specification<Event> spec = Specification.unrestricted();
        if (timeframe == EventTimeframe.UPCOMING) {
            spec = spec.and(EventSpecifications.startsAfter(now));
        } else if (timeframe == EventTimeframe.PAST) {
            spec = spec.and(EventSpecifications.startsBeforeOrEqual(now));
        }
        List<Event> events = eventRepository.findAll(spec, Sort.by(Sort.Order.desc("startsAt"), Sort.Order.asc("id")));
        if (events.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = events.stream().map(Event::getId).toList();
        Map<UUID, Long> registered = seatCounts(ids, EventRegistrationStatus.REGISTERED);
        Map<UUID, Long> waitlisted = seatCounts(ids, EventRegistrationStatus.WAITLISTED);
        Map<UUID, Long> cancellations = new HashMap<>();
        for (ActivityCount row : registrationActivityRepository.countByEventIdInAndTypeIn(
                ids, List.of(RegistrationActivityType.CANCELLED, RegistrationActivityType.LEFT_WAITLIST))) {
            cancellations.merge(row.eventId(), row.count(), Long::sum);
        }

        return events.stream()
                .map(event -> {
                    long held = registered.getOrDefault(event.getId(), 0L);
                    return new EventOccupancyResponse(
                            event.getId(),
                            event.getTitle(),
                            event.getStartsAt(),
                            event.getSeatLimit(),
                            (int) held,
                            waitlisted.getOrDefault(event.getId(), 0L).intValue(),
                            cancellations.getOrDefault(event.getId(), 0L).intValue(),
                            occupancyPercent(held, event.getSeatLimit()));
                })
                .toList();
    }

    private Map<UUID, Long> seatCounts(List<UUID> eventIds, EventRegistrationStatus status) {
        Map<UUID, Long> counts = new HashMap<>();
        for (EventSeatCount row : eventRegistrationRepository.countByEventIdInAndStatus(eventIds, status)) {
            counts.put(row.eventId(), row.seatsTaken());
        }
        return counts;
    }

    /**
     * Runs from publication to today, or to the start of an event that has already begun, since nobody can
     * sign up or cancel after that. Activity outside that span — sign-ups imported from before the event's
     * creation date was tracked, say — stretches it rather than going missing.
     */
    private List<DailyRegistrationsResponse> daily(Event event, List<ActivityOccurrence> occurrences) {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        LocalDate startDay = LocalDate.ofInstant(event.getStartsAt(), ZONE);
        LocalDate last = startDay.isBefore(today) ? startDay : today;
        LocalDate first = LocalDate.ofInstant(event.getCreatedAt(), ZONE);
        if (first.isAfter(last)) {
            first = last;
        }

        Map<LocalDate, int[]> byDay = new HashMap<>();
        for (ActivityOccurrence occurrence : occurrences) {
            LocalDate day = LocalDate.ofInstant(occurrence.occurredAt(), ZONE);
            int[] counts = byDay.computeIfAbsent(day, d -> new int[2]);
            switch (occurrence.type()) {
                case SIGNED_UP, WAITLISTED -> counts[0]++;
                case CANCELLED, LEFT_WAITLIST -> counts[1]++;
                case PROMOTED -> {}
            }
            if (day.isBefore(first)) {
                first = day;
            }
            if (day.isAfter(last)) {
                last = day;
            }
        }

        List<DailyRegistrationsResponse> series = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            int[] counts = byDay.getOrDefault(day, new int[2]);
            series.add(new DailyRegistrationsResponse(day, counts[0], counts[1]));
        }
        return series;
    }

    private static long count(Map<RegistrationActivityType, Long> byType, RegistrationActivityType type) {
        return byType.getOrDefault(type, 0L);
    }

    private static Integer occupancyPercent(long registered, Integer seatLimit) {
        if (seatLimit == null) {
            return null;
        }
        if (seatLimit == 0 && registered == 0) {
            return 100;
        }
        return (int) Math.round(registered * 100.0 / Math.max(seatLimit, 1));
    }
}
