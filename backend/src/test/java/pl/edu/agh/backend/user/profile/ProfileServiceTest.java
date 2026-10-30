package pl.edu.agh.backend.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.storage.AvatarPresignRequest;
import pl.edu.agh.backend.storage.PresignedUploadResponse;
import pl.edu.agh.backend.storage.StorageService;
import pl.edu.agh.backend.user.AlumniTag;
import pl.edu.agh.backend.user.AlumniTagRepository;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlumniTagRepository alumniTagRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void updateProfileClearsBlankStrings() {
        User user = new User();
        user.setKeycloakId("kc-1");
        user.setCompany("ACME");
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(applicationRepository.findFirstByApplicantIdAndStatusOrderByReviewedAtDesc(
                        user.getId(), pl.edu.agh.backend.application.ApplicationStatus.APPROVED))
                .thenReturn(java.util.Optional.empty());

        var response = profileService.updateProfile(
                "kc-1", new UpdateProfileRequest(null, null, "", null, null, null, null, null, null));

        assertThat(response.company()).isNull();
        assertThat(user.getCompany()).isNull();
    }

    @Test
    void updateProfileStoresAnAvatarTheUserOwns() {
        User user = userWithId("kc-1");
        String key = "avatars/" + user.getId() + "/" + UUID.randomUUID() + ".png";
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(storageService.isConfigured()).thenReturn(true);
        when(storageService.isAvatarKeyOwnedBy(user.getId(), key)).thenReturn(true);
        when(storageService.resolveUrl(key)).thenReturn("https://r2.example/signed");

        var response = profileService.updateProfile("kc-1", avatarRequest("  " + key + " "));

        assertThat(user.getAvatarKey()).isEqualTo(key);
        assertThat(response.avatarUrl()).isEqualTo("https://r2.example/signed");
    }

    @Test
    void getProfileReturnsASignedAvatarUrlNotTheStoredKey() {
        User user = userWithId("kc-1");
        user.setAvatarKey("avatars/k.png");
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(storageService.resolveUrl("avatars/k.png")).thenReturn("https://r2.example/signed");

        assertThat(profileService.getProfile("kc-1").avatarUrl()).isEqualTo("https://r2.example/signed");
    }

    @Test
    void updateProfileRejectsAnAvatarTheUserDoesNotOwn() {
        User user = userWithId("kc-1");
        user.setAvatarKey("avatars/old.png");
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(storageService.isConfigured()).thenReturn(true);
        when(storageService.isAvatarKeyOwnedBy(user.getId(), "avatars/someone-else/a.png"))
                .thenReturn(false);

        assertThatThrownBy(() -> profileService.updateProfile("kc-1", avatarRequest("avatars/someone-else/a.png")))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(user.getAvatarKey()).isEqualTo("avatars/old.png");
    }

    @Test
    void updateProfileAnswers503ForAnAvatarWhenStorageIsNotConfigured() {
        User user = userWithId("kc-1");
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(storageService.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> profileService.updateProfile("kc-1", avatarRequest("avatars/a.png")))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void updateProfileRemovesTheAvatarOnBlankAndKeepsItWhenOmitted() {
        User user = userWithId("kc-1");
        user.setAvatarKey("avatars/old.png");
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));

        profileService.updateProfile("kc-1", avatarRequest(null));
        assertThat(user.getAvatarKey()).isEqualTo("avatars/old.png");

        profileService.updateProfile("kc-1", avatarRequest("  "));
        assertThat(user.getAvatarKey()).isNull();
        verify(storageService, never()).isAvatarKeyOwnedBy(any(), any());
    }

    @Test
    void presignAvatarUploadIsIssuedForTheCallersOwnUserId() {
        User user = userWithId("kc-1");
        var request = new AvatarPresignRequest("image/png", 1024L);
        var expected = new PresignedUploadResponse("https://r2/x", java.util.Map.of(), "k", null);
        when(userRepository.findByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(storageService.presignAvatar(user.getId(), "image/png", 1024L)).thenReturn(expected);

        assertThat(profileService.presignAvatarUpload("kc-1", request)).isSameAs(expected);
    }

    @Test
    void presignAvatarUploadFailsForUnknownUser() {
        when(userRepository.findByKeycloakId("kc-x")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> profileService.presignAvatarUpload("kc-x", new AvatarPresignRequest("image/png", 1L)))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateTagsReplacesAssignedTags() {
        User user = new User();
        user.setKeycloakId("kc-1");
        AlumniTag tag = AlumniTag.builder().name("Java").build();
        tag.setId(UUID.randomUUID());
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(alumniTagRepository.findByNameKeyIn(List.of("java"))).thenReturn(List.of(tag));

        var tags = profileService.updateTags("kc-1", Set.of("Java"));

        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst().name()).isEqualTo("Java");
    }

    private static User userWithId(String keycloakId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setKeycloakId(keycloakId);
        return user;
    }

    private static UpdateProfileRequest avatarRequest(String avatarKey) {
        return new UpdateProfileRequest(null, null, null, null, null, null, null, null, avatarKey);
    }
}
