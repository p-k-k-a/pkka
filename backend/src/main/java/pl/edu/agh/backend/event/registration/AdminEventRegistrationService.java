package pl.edu.agh.backend.event.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.dto.AdminEventRegistrationResponse;

@Service
@RequiredArgsConstructor
public class AdminEventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;

    /** Seat holders in sign-up order, then the waitlist in the order it will be promoted. */
    @Transactional(readOnly = true)
    public List<AdminEventRegistrationResponse> list(UUID eventId) {
        if (eventRepository.findById(eventId).isEmpty()) {
            throw new EventNotFoundException(eventId);
        }
        List<EventRegistration> registrations =
                eventRegistrationRepository.findByEventIdOrderByRegisteredAtAscIdAsc(eventId);

        List<AdminEventRegistrationResponse> seated = new ArrayList<>();
        List<AdminEventRegistrationResponse> queued = new ArrayList<>();
        for (EventRegistration registration : registrations) {
            if (registration.getStatus() == EventRegistrationStatus.WAITLISTED) {
                queued.add(AdminEventRegistrationResponse.from(registration, queued.size() + 1));
            } else {
                seated.add(AdminEventRegistrationResponse.from(registration, null));
            }
        }
        seated.addAll(queued);
        return seated;
    }
}
