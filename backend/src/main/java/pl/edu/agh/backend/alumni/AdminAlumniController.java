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
@RequestMapping("/api/admin/alumni")
@RequiredArgsConstructor
@Tag(
        name = "Admin Alumni",
        description = "Alumni directory for admins, including fields alumni hide — requires ADMIN role")
public class AdminAlumniController {

    private final AlumniService alumniService;

    @GetMapping
    @Operation(summary = "Search and list alumni as an admin", description = """
                    Same filters, sorting and paging as `GET /api/alumni`, but alumni who hide their name are
                    listed too and `q` matches their name. `visibility` says what each alumnus hides from other
                    alumni.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "400",
                description = "Unsupported sort property",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<AdminAlumniListItemResponse> listAlumniForAdmin(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<UUID> tagIds,
            @RequestParam(required = false) Boolean mentor,
            @RequestParam(required = false) Integer graduationYearFrom,
            @RequestParam(required = false) Integer graduationYearTo,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return alumniService.searchForAdmin(q, tagIds, mentor, graduationYearFrom, graduationYearTo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an alumni profile as an admin, including fields the alumnus hides")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                description = "No user with the given id, or that user is not a verified alumnus "
                        + "(no approved application)",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AlumniProfileResponse getAlumniProfileForAdmin(@PathVariable UUID id) {
        return alumniService.getProfileForAdmin(id);
    }
}
