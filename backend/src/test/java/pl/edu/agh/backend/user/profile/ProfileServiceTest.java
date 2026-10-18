package pl.edu.agh.backend.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.agh.backend.application.ApplicationRepository;
import pl.edu.agh.backend.user.User;
import pl.edu.agh.backend.user.UserRepository;
import pl.edu.agh.backend.user.UserTag;
import pl.edu.agh.backend.user.UserTagRepository;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserTagRepository userTagRepository;

    @Mock
    private ApplicationRepository applicationRepository;

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

        var response =
                profileService.updateProfile("kc-1", new UpdateProfileRequest(null, null, "", null, null, null, null));

        assertThat(response.company()).isNull();
        assertThat(user.getCompany()).isNull();
    }

    @Test
    void updateTagsReplacesAssignedTags() {
        User user = new User();
        user.setKeycloakId("kc-1");
        UserTag tag = UserTag.builder().name("Java").build();
        tag.setId(UUID.randomUUID());
        when(userRepository.findWithTagsByKeycloakId("kc-1")).thenReturn(java.util.Optional.of(user));
        when(userTagRepository.findByNameKeyIn(List.of("java"))).thenReturn(List.of(tag));

        var tags = profileService.updateTags("kc-1", Set.of("Java"));

        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst().name()).isEqualTo("Java");
    }
}
