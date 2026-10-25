package pl.edu.agh.backend.user.profile;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.infrastructure.validation.TagNames;
import pl.edu.agh.backend.user.AlumniTag;
import pl.edu.agh.backend.user.AlumniTagRepository;
import pl.edu.agh.backend.user.AlumniTagResponse;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final AlumniTagRepository alumniTagRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String keycloakId) {
        User user = loadWithTags(keycloakId);
        return ProfileResponse.from(user, education(user));
    }

    @Transactional
    public ProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = loadWithTags(keycloakId);
        if (request.bio() != null) {
            user.setBio(blankToNull(request.bio()));
        }
        if (request.currentPosition() != null) {
            user.setCurrentPosition(blankToNull(request.currentPosition()));
        }
        if (request.company() != null) {
            user.setCompany(blankToNull(request.company()));
        }
        if (request.linkedinUrl() != null) {
            user.setLinkedinUrl(blankToNull(request.linkedinUrl()));
        }
        if (request.githubUrl() != null) {
            user.setGithubUrl(blankToNull(request.githubUrl()));
        }
        if (request.phoneNumber() != null) {
            user.setPhoneNumber(blankToNull(request.phoneNumber()));
        }
        if (request.willingToMentor() != null) {
            user.setWillingToMentor(request.willingToMentor());
        }
        if (request.visibility() != null) {
            var visibility = request.visibility();
            if (visibility.name() != null) {
                user.setShowName(visibility.name());
            }
            if (visibility.email() != null) {
                user.setShowEmail(visibility.email());
            }
            if (visibility.discord() != null) {
                user.setShowDiscord(visibility.discord());
            }
            if (visibility.phone() != null) {
                user.setShowPhone(visibility.phone());
            }
        }
        return ProfileResponse.from(user, education(user));
    }

    private AlumnEducation education(User user) {
        return AlumnEducation.from(applicationRepository
                .findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(user.getId(), ApplicationStatus.APPROVED)
                .orElse(null));
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    @Transactional(readOnly = true)
    public List<AlumniTagResponse> getTags(String keycloakId) {
        return loadWithTags(keycloakId).getTags().stream()
                .map(AlumniTagResponse::from)
                .sorted(Comparator.comparing(AlumniTagResponse::name))
                .toList();
    }

    @Transactional
    public List<AlumniTagResponse> updateTags(String keycloakId, Set<String> names) {
        User user = loadWithTags(keycloakId);
        names.forEach(name -> alumniTagRepository.insertIfAbsent(UUID.randomUUID(), name));
        List<AlumniTag> tags = names.isEmpty()
                ? List.of()
                : alumniTagRepository.findByNameKeyIn(
                        names.stream().map(TagNames::key).toList());
        user.getTags().clear();
        user.getTags().addAll(tags);
        return tags.stream()
                .map(AlumniTagResponse::from)
                .sorted(Comparator.comparing(AlumniTagResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private User loadWithTags(String keycloakId) {
        return userRepository
                .findWithTagsByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
    }
}
