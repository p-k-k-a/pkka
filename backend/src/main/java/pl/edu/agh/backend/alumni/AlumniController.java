package pl.edu.agh.backend.alumni;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
@Tag(name = "Alumni", description = "Alumni directory — requires VERIFIED_ALUMN role")
public class AlumniController {

    private final AlumniService alumniService;

    @GetMapping
    @Operation(summary = "Search and list alumni", description = """
                    Paginated, filterable alumni directory of verified alumni only (users without an approved
                    application never appear here, even if they hold a local account). `q` matches
                    (case-insensitively, substring) against first/last name (unless the alumnus hides their name),
                    current position, company and assigned skill tag names. `tagIds` matches alumni who have at
                    least one of the given skill tags (OR semantics). `mentor=true`/`mentor=false` restricts to
                    alumni who are/aren't willing to mentor. `graduationYear` restricts to alumni who graduated in
                    that exact year. All filters are combined with AND.
                    Unknown tag IDs simply match no one instead of returning an error, since this is a search
                    filter rather than a write operation.
                    """)
    public Page<AlumniListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<UUID> tagIds,
            @RequestParam(required = false) Boolean mentor,
            @RequestParam(required = false) Integer graduationYear,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return alumniService.search(q, tagIds, mentor, graduationYear, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public alumni profile by user id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "No user with the given id, or that user is not a verified alumnus "
                        + "(no approved application)",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AlumniProfileResponse getAlumniProfile(@PathVariable UUID id) {
        return alumniService.getProfile(id);
    }
}
