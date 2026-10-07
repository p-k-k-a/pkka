package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/materials")
@RequiredArgsConstructor
@Tag(name = "Admin Materials", description = "Material management for administrators")
public class AdminMaterialController {

    private final AdminMaterialService adminMaterialService;

    @GetMapping
    @Operation(summary = "List materials with pagination and optional filters")
    public Page<AdminMaterialResponse> listAdminMaterials(
            @Parameter(description = "Matches the title, the description or the linked event's title")
                    @RequestParam(required = false)
                    String q,
            @RequestParam(required = false) MaterialType type,
            @RequestParam(required = false) UUID eventId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return adminMaterialService.list(q, Optional.ofNullable(type), Optional.ofNullable(eventId), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single material")
    @ApiResponse(responseCode = "200", description = "Material details")
    @ApiResponse(responseCode = "404", description = "Material not found", content = @Content)
    public AdminMaterialResponse getAdminMaterial(@PathVariable UUID id) {
        return adminMaterialService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a material from an external URL")
    @ApiResponse(responseCode = "201", description = "Material created")
    public AdminMaterialResponse createAdminMaterial(@Valid @RequestBody MaterialRequest request) {
        return adminMaterialService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a material")
    @ApiResponse(responseCode = "200", description = "Material updated")
    @ApiResponse(responseCode = "404", description = "Material not found", content = @Content)
    public AdminMaterialResponse updateAdminMaterial(
            @PathVariable UUID id, @Valid @RequestBody MaterialRequest request) {
        return adminMaterialService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a material permanently")
    @ApiResponse(responseCode = "204", description = "Material deleted")
    @ApiResponse(responseCode = "404", description = "Material not found", content = @Content)
    public void deleteAdminMaterial(@PathVariable UUID id) {
        adminMaterialService.delete(id);
    }
}
