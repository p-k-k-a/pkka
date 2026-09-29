package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/alumni/materials")
@RequiredArgsConstructor
@Tag(name = "Materials", description = "Learning materials available to verified alumni")
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    @Operation(summary = "List materials with pagination and optional filters")
    public Page<MaterialResponse> listMaterials(
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) UUID eventId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable,
            Caller caller) {
        return materialService.list(Optional.ofNullable(type), Optional.ofNullable(eventId), pageable, caller);
    }
}
