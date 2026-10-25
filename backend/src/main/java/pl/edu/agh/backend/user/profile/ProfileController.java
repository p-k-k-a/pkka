package pl.edu.agh.backend.user.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;

@RestController
@RequestMapping("/api/profiles/me")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Own user profile management — requires USER role")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get own profile")
    public ProfileResponse getMyProfile(Caller caller) {
        return profileService.getProfile(caller.requireKeycloakId());
    }

    @PatchMapping
    @Operation(
            summary = "Update own profile",
            description = "Partial update: null/omitted fields are left unchanged; a blank string clears the value")
    public ProfileResponse updateMyProfile(Caller caller, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(caller.requireKeycloakId(), request);
    }
}
