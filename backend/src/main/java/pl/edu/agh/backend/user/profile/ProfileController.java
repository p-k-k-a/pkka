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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pl.edu.agh.backend.event.TagResponse;
import pl.edu.agh.backend.user.UserPrincipalExtractor;
import pl.edu.agh.backend.user.profile.dto.ProfileResponse;
import pl.edu.agh.backend.user.profile.dto.UpdateProfileRequest;
import pl.edu.agh.backend.user.profile.dto.UpdateTagsRequest;

@RestController
@RequestMapping("/api/profiles/me")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Own user profile management — requires USER role")
public class ProfileController {

    private final ProfileService profileService;
    private final UserPrincipalExtractor principalExtractor;

    @GetMapping
    @Operation(summary = "Get own profile")
    public ProfileResponse getMyProfile(Authentication authentication) {
        return profileService.getProfile(keycloakId(authentication));
    }

    @PatchMapping
    @Operation(
            summary = "Update own profile",
            description = "Partial update: null or omitted fields clear the stored value")
    public ProfileResponse updateMyProfile(
            Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(keycloakId(authentication), request);
    }

    @GetMapping("/tags")
    @Operation(summary = "Get own assigned tags")
    public List<TagResponse> getMyTags(Authentication authentication) {
        return profileService.getTags(keycloakId(authentication));
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
    public List<TagResponse> updateMyTags(
            Authentication authentication, @Valid @RequestBody UpdateTagsRequest request) {
        return profileService.updateTags(keycloakId(authentication), request.tagIds());
    }

    private String keycloakId(Authentication authentication) {
        return principalExtractor
                .extract(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .keycloakId();
    }
}
