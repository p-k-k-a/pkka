package pl.edu.agh.backend.user.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.user.CurrentUserService;
import pl.edu.agh.backend.user.UserTagResponse;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;
import pl.edu.agh.backend.user.profile.dto.UpdateTagsRequest;

@RestController
@RequestMapping("/api/profiles/me")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Own user profile management — requires USER role")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "Get own profile")
    public ProfileResponse getMyProfile(Authentication authentication) {
        return profileService.getProfile(currentUserService.requireKeycloakId(authentication));
    }

    @PatchMapping
    @Operation(
            summary = "Update own profile",
            description = "Partial update: null/omitted fields are left unchanged; a blank string clears the value")
    public ProfileResponse updateMyProfile(
            Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(currentUserService.requireKeycloakId(authentication), request);
    }

    @GetMapping("/tags")
    @Operation(summary = "Get own assigned tags")
    public List<UserTagResponse> getMyTags(Authentication authentication) {
        return profileService.getTags(currentUserService.requireKeycloakId(authentication));
    }

    @PutMapping("/tags")
    @Operation(summary = "Replace own tags (full replacement of the assigned set)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "400",
                description = "One or more tag IDs do not exist",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<UserTagResponse> updateMyTags(
            Authentication authentication, @Valid @RequestBody UpdateTagsRequest request) {
        return profileService.updateTags(currentUserService.requireKeycloakId(authentication), request.tagIds());
    }
}
