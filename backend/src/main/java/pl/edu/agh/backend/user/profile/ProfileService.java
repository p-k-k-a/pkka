package pl.edu.agh.backend.user.profile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.controller.dto.TagResponse;
import pl.edu.agh.backend.event.Tag;
import pl.edu.agh.backend.event.TagRepository;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String keycloakId) {
        return ProfileResponse.from(loadWithTags(keycloakId));
    }

    @Transactional
    public ProfileResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = loadWithTags(keycloakId);
        user.setCurrentPosition(request.currentPosition());
        user.setCompany(request.company());
        user.setLinkedinUrl(request.linkedinUrl());
        user.setGithubUrl(request.githubUrl());
        return ProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTags(String keycloakId) {
        return loadWithTags(keycloakId).getTags().stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }

    @Transactional
    public List<TagResponse> updateTags(String keycloakId, Set<UUID> tagIds) {
        User user = loadWithTags(keycloakId);
        List<Tag> found = tagRepository.findAllById(tagIds);
        if (found.size() != tagIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more tag IDs are invalid");
        }
        user.getTags().clear();
        user.getTags().addAll(new HashSet<>(found));
        return user.getTags().stream()
                .map(TagResponse::from)
                .sorted(Comparator.comparing(TagResponse::name))
                .toList();
    }

    private User loadWithTags(String keycloakId) {
        return userRepository
                .findWithTagsByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));
    }
}
