package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@Tag(name = "Admin Applications", description = "Reviewer actions on alumnus applications")
public class AdminApplicationController {

    private final AdminApplicationService adminApplicationService;

    @GetMapping
    @Operation(summary = "List applications with full details, filtered by status (newest first)")
    public Page<AdminApplicationResponseDto> listAdminApplications(
            @RequestParam(defaultValue = "UNDER_REVIEW") ApplicationStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminApplicationService.list(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full details of a single application")
    @ApiResponse(responseCode = "200", description = "Application details")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public AdminApplicationResponseDto getAdminApplication(@PathVariable UUID id) {
        return adminApplicationService.get(id);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an application and grant the verified alumn role")
    @ApiResponse(responseCode = "200", description = "Application approved")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Application is not under review", content = @Content)
    public ApplicationResponseDto approve(@PathVariable UUID id, Authentication authentication) {
        return adminApplicationService.approve(authentication, id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an application with an optional reason")
    @ApiResponse(responseCode = "200", description = "Application rejected")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Application is not under review", content = @Content)
    public ApplicationResponseDto reject(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RejectApplicationRequestDto request,
            Authentication authentication) {
        String reason = request == null ? null : request.reason();
        return adminApplicationService.reject(authentication, id, reason);
    }
}
