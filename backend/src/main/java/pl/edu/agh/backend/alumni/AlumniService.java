package pl.edu.agh.backend.alumni;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AlumniService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public AlumniProfileResponse getProfile(UUID id) {
        User user = userRepository.findWithTagsById(id).orElseThrow(() -> new AlumniNotFoundException(id));
        AlumnEducation education = AlumnEducation.from(applicationRepository
                .findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(id, ApplicationStatus.APPROVED)
                .orElse(null));
        return AlumniProfileResponse.from(user, education);
    }
}
