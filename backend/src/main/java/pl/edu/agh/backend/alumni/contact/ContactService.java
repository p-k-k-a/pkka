package pl.edu.agh.backend.alumni.contact;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.alumni.AlumniNotFoundException;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.CallerUserService;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CallerUserService callerUserService;

    public List<ContactResponse> list(Caller caller) {
        return callerUserService
                .findId(caller)
                .map(contactRepository::findByOwnerIdAndContactShowNameTrueOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(contact -> ContactResponse.from(contact.getContact()))
                .toList();
    }

    public boolean isContact(Caller caller, UUID alumnId) {
        return callerUserService
                .findId(caller)
                .map(ownerId -> contactRepository.existsByOwnerIdAndContactId(ownerId, alumnId))
                .orElse(false);
    }

    @Transactional
    public void add(Caller caller, UUID alumnId) {
        User owner = callerUserService.getOrCreate(caller);
        if (owner.getId().equals(alumnId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot add yourself to your contacts");
        }
        if (applicationRepository
                .findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(alumnId, ApplicationStatus.APPROVED)
                .isEmpty()) {
            throw new AlumniNotFoundException(alumnId);
        }
        if (contactRepository.existsByOwnerIdAndContactId(owner.getId(), alumnId)) {
            return;
        }
        contactRepository.save(new Contact(owner, userRepository.getReferenceById(alumnId)));
    }

    @Transactional
    public void remove(Caller caller, UUID alumnId) {
        callerUserService
                .findId(caller)
                .ifPresent(ownerId -> contactRepository.deleteByOwnerIdAndContactId(ownerId, alumnId));
    }
}
