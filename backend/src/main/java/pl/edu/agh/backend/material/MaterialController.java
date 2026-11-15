package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
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
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Tag(
        name = "Materials",
        description = "Learning materials for signed-in users; each caller gets the ones their audience may see")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @Operation(
            summary = "List the materials the caller may see, with pagination and optional filters",
            description = "A material linked to an event is visible to whoever may see that event; one without an "
                    + "event is visible to its own audience. Verified alumni therefore see more than other users.")
    public Page<MaterialResponse> listMaterials(
            @Parameter(description = "Matches the title, the description or the linked event's title")
                    @RequestParam(required = false)
                    String q,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) UUID eventId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            Caller caller) {
        return materialService.list(q, Optional.ofNullable(type), Optional.ofNullable(eventId), pageable, caller);
    }
}
