package pl.edu.agh.backend.user.alumni;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.user.alumni.dto.AlumniListItemResponse;

@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
@Tag(name = "Alumni", description = "Alumni directory — requires VERIFIED_ALUMN role")
public class AlumniController {

    private final AlumniService alumniService;

    @GetMapping
    @Operation(summary = "Search and list alumni", description = """
                    Paginated, filterable alumni directory. `q` matches (case-insensitively, substring) against
                    current position, company and assigned tag names. `tagIds` matches alumni who have at least
                    one of the given tags (OR semantics). Both filters can be combined (AND).
                    Unknown tag IDs simply match no one instead of returning an error, since this is a search
                    filter rather than a write operation.
                    """)
    public Page<AlumniListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Set<UUID> tagIds,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return alumniService.search(q, tagIds, pageable);
    }
}
