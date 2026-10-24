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
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.storage.AvatarPresignRequest;
import pl.edu.agh.backend.storage.PresignedUploadResponse;
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

    @GetMapping
    @Operation(summary = "Get own profile")
    public ProfileResponse getMyProfile(Caller caller) {
        return profileService.getProfile(caller.requireKeycloakId());
    }

    @PatchMapping
    @Operation(
            summary = "Update own profile",
            description = "Partial update: null/omitted fields are left unchanged; a blank string clears the value")
    @ApiResponse(
            responseCode = "400",
            description = "Validation failed, or avatarKey is not an avatar uploaded by this user",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "503",
            description = "avatarKey was sent but object storage is not configured",
            content = @Content)
    public ProfileResponse updateMyProfile(Caller caller, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(caller.requireKeycloakId(), request);
    }

    @PostMapping("/avatar/presign")
    @Operation(
            summary = "Get a presigned URL to upload own avatar straight to object storage",
            description = "PUT the image to `uploadUrl` with the returned headers, then send `objectKey` as "
                    + "`avatarKey` in the profile update; the profile then returns a short-lived signed `avatarUrl`. "
                    + "This is the only object-storage upload for non-admin users (ROLE_USER); admins use "
                    + "POST /api/admin/files/presign for materials and event covers.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Upload URL issued"),
        @ApiResponse(responseCode = "413", description = "Image larger than the limit", content = @Content),
        @ApiResponse(responseCode = "415", description = "Content type is not jpeg, png or webp", content = @Content),
        @ApiResponse(responseCode = "503", description = "Object storage is not configured", content = @Content)
    })
    public PresignedUploadResponse presignAvatar(Caller caller, @Valid @RequestBody AvatarPresignRequest request) {
        return profileService.presignAvatarUpload(caller.requireKeycloakId(), request);
    }

    @GetMapping("/tags")
    @Operation(summary = "Get own assigned tags")
    public List<UserTagResponse> getMyTags(Caller caller) {
        return profileService.getTags(caller.requireKeycloakId());
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
    public List<UserTagResponse> updateMyTags(Caller caller, @Valid @RequestBody UpdateTagsRequest request) {
        return profileService.updateTags(caller.requireKeycloakId(), request.tagIds());
    }
}
