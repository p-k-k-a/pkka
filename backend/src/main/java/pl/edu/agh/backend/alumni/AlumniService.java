package pl.edu.agh.backend.alumni;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.edu.agh.backend.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AlumniService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AlumniProfileResponse getProfile(UUID id) {
        return userRepository
                .findWithTagsById(id)
                .map(AlumniProfileResponse::from)
                .orElseThrow(() -> new AlumniNotFoundException(id));
    }
}
