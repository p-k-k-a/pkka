package pl.edu.agh.backend.alumni;

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
import pl.edu.agh.backend.infrastructure.validation.TagNames;
import pl.edu.agh.backend.security.Caller;
import pl.edu.agh.backend.user.AlumniTagRepository;
import pl.edu.agh.backend.user.AlumniTagResponse;
import pl.edu.agh.backend.user.profile.ProfileService;
import pl.edu.agh.backend.user.profile.dto.UpdateTagsRequest;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
@Tag(name = "Alumni Tags", description = "Alumni skill tags — requires VERIFIED_ALUMN role")
public class AlumniTagController {

    private final AlumniTagRepository alumniTagRepository;
    private final ProfileService profileService;

    @GetMapping("/tags")
    @Operation(
            summary = "List all alumni skill tags with how many listed alumni have each",
            description = "Most used first, then alphabetical")
    public List<AlumniTagResponse> listAlumniTags() {
        return alumniTagRepository.findCatalog().stream()
                .sorted(AlumniTagResponse.MOST_USED_FIRST)
                .toList();
    }

    @GetMapping("/me/tags")
    @Operation(summary = "Get own assigned tags")
    public List<AlumniTagResponse> getMyTags(Caller caller) {
        return profileService.getTags(caller.requireKeycloakId());
    }

    @PutMapping("/me/tags")
    @Operation(summary = "Replace own tags, creating any named tag that is not in the catalog yet")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid tag name or more than " + TagNames.MAX_PER_USER + " tags",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<AlumniTagResponse> updateMyTags(Caller caller, @Valid @RequestBody UpdateTagsRequest request) {
        return profileService.updateTags(caller.requireKeycloakId(), request.names());
    }
}
