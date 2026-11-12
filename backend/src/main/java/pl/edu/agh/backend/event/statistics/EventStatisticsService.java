package pl.edu.agh.backend.event.statistics;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.Event;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository;
import pl.edu.agh.backend.event.registration.EventRegistrationRepository.EventSeatCount;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository.ActivityCount;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository.ActivityOccurrence;
import pl.edu.agh.backend.event.registration.RegistrationActivityRepository.SourceCount;
import pl.edu.agh.backend.event.registration.RegistrationActivityType;
import pl.edu.agh.backend.event.registration.RegistrationSource;

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
        List<DailyRegistrationsResponse> daily = daily(event, occurrences);
        Map<RegistrationSource, Long> signUpsBySource = occurrences.stream()
                .filter(occurrence -> isSignUp(occurrence.type()) && occurrence.source() != null)
                .collect(Collectors.groupingBy(ActivityOccurrence::source, Collectors.counting()));

        long registered =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
        long waitlisted =
                eventRegistrationRepository.countByEventIdAndStatus(eventId, EventRegistrationStatus.WAITLISTED);

        return new EventStatisticsResponse(
                event.getSeatLimit(),
                (int) registered,
                (int) waitlisted,
                daily.stream().mapToInt(DailyRegistrationsResponse::signUps).sum(),
                daily.stream()
                        .mapToInt(DailyRegistrationsResponse::cancellations)
                        .sum(),
                daily,
                bySource(signUpsBySource));
    }

    @Transactional(readOnly = true)
    public List<EventOccupancyResponse> compareOccupancy() {
        List<Event> events = eventRepository.findAll(Sort.by(Sort.Order.desc("startsAt"), Sort.Order.asc("id")));
        if (events.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = events.stream().map(Event::getId).toList();
        Map<UUID, Long> registered = seatCounts(ids, EventRegistrationStatus.REGISTERED);
        Map<UUID, Long> waitlisted = seatCounts(ids, EventRegistrationStatus.WAITLISTED);
        Map<UUID, Long> cancellations = new HashMap<>();
        for (ActivityCount row : registrationActivityRepository.countCancellationsByEventIdIn(ids)) {
            cancellations.put(row.eventId(), row.count());
        }
        Map<UUID, Map<RegistrationSource, Long>> signUpsBySource = new HashMap<>();
        for (SourceCount row : registrationActivityRepository.countSignUpsBySourceByEventIdIn(ids)) {
            signUpsBySource
                    .computeIfAbsent(row.eventId(), id -> new EnumMap<>(RegistrationSource.class))
                    .put(row.source(), row.count());
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
                            bySource(signUpsBySource.getOrDefault(event.getId(), Map.of())));
                })
                .toList();
    }

    private static List<SignUpSourceResponse> bySource(Map<RegistrationSource, Long> counts) {
        return Arrays.stream(RegistrationSource.values())
                .map(source -> new SignUpSourceResponse(
                        source, counts.getOrDefault(source, 0L).intValue()))
                .toList();
    }

    private static boolean isSignUp(RegistrationActivityType type) {
        return type == RegistrationActivityType.SIGNED_UP || type == RegistrationActivityType.WAITLISTED;
    }

    private Map<UUID, Long> seatCounts(List<UUID> eventIds, EventRegistrationStatus status) {
        Map<UUID, Long> counts = new HashMap<>();
        for (EventSeatCount row : eventRegistrationRepository.countByEventIdInAndStatus(eventIds, status)) {
            counts.put(row.eventId(), row.seatsTaken());
        }
        return counts;
    }

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
                case CANCELLED -> counts[1]++;
                case LEFT_WAITLIST -> {}
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
}
