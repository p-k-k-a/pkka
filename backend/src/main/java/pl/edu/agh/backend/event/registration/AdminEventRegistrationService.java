package pl.edu.agh.backend.event.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.event.EventNotFoundException;
import pl.edu.agh.backend.event.EventRepository;
import pl.edu.agh.backend.event.registration.dto.AdminEventRegistrationResponse;

@Service
@RequiredArgsConstructor
public class AdminEventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public List<AdminEventRegistrationResponse> list(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        List<EventRegistration> registrations =
                eventRegistrationRepository.findByEventIdOrderByRegisteredAtAscIdAsc(eventId);
        Set<UUID> alumni = applicationRepository.findApplicantIdsByStatus(
                registrations.stream().map(r -> r.getUser().getId()).toList(), ApplicationStatus.APPROVED);

        List<AdminEventRegistrationResponse> seated = new ArrayList<>();
        List<AdminEventRegistrationResponse> queued = new ArrayList<>();
        for (EventRegistration registration : registrations) {
            boolean alumnus = alumni.contains(registration.getUser().getId());
            if (registration.getStatus() == EventRegistrationStatus.WAITLISTED) {
                queued.add(AdminEventRegistrationResponse.from(registration, queued.size() + 1, alumnus));
            } else {
                seated.add(AdminEventRegistrationResponse.from(registration, null, alumnus));
            }
        }
        seated.addAll(queued);
        return seated;
    }
}
