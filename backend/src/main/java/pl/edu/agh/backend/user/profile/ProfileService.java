package pl.edu.agh.backend.user.profile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.application.AlumnEducation;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.application.ApplicationStatus;
import pl.edu.agh.backend.infrastructure.validation.TagNames;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTag;
import pl.edu.agh.backend.user.UserTagRepository;
import pl.edu.agh.backend.user.UserTagResponse;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MAX_TAGS = 20;

    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
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
    public List<UserTagResponse> getTags(String keycloakId) {
        return loadWithTags(keycloakId).getTags().stream()
                .map(UserTagResponse::from)
                .sorted(Comparator.comparing(UserTagResponse::name))
                .toList();
    }

    @Transactional
    public List<UserTagResponse> updateTags(String keycloakId, Set<UUID> tagIds, Set<String> names) {
        User user = loadWithTags(keycloakId);
        Set<UserTag> next = new HashSet<>(userTagRepository.findAllById(tagIds));
        if (next.size() != tagIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tag IDs are invalid");
        }
        next.addAll(findOrCreateByName(user, names));
        if (next.size() > MAX_TAGS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_TAGS + " tags are allowed");
        }

        Set<UUID> removedIds = user.getTags().stream()
                .filter(tag -> !next.contains(tag))
                .map(UserTag::getId)
                .collect(Collectors.toSet());
        user.getTags().clear();
        user.getTags().addAll(next);
        if (!removedIds.isEmpty()) {
            userTagRepository.deleteUnusedUserCreated(removedIds);
        }

        return next.stream()
                .map(UserTagResponse::from)
                .sorted(Comparator.comparing(UserTagResponse::name))
                .toList();
    }

    private List<UserTag> findOrCreateByName(User user, Set<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        Map<String, String> nameByKey = new LinkedHashMap<>();
        names.forEach(name -> nameByKey.putIfAbsent(TagNames.key(name), name));

        List<UserTag> existing = userTagRepository.findByNameKeyIn(nameByKey.keySet());
        if (existing.size() == nameByKey.size()) {
            return existing;
        }
        Set<String> existingKeys =
                existing.stream().map(tag -> TagNames.key(tag.getName())).collect(Collectors.toSet());
        nameByKey.forEach((key, name) -> {
            if (!existingKeys.contains(key)) {
                userTagRepository.insertIfAbsent(UUID.randomUUID(), name, user.getId());
            }
        });
        return userTagRepository.findByNameKeyIn(nameByKey.keySet());
    }

    private User loadWithTags(String keycloakId) {
        return userRepository
                .findWithTagsByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
    }
}
