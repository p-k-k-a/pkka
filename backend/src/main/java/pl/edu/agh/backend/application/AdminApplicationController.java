package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@Tag(name = "Admin Applications", description = "Reviewer actions on alumnus applications")
public class AdminApplicationController {

    private final AdminApplicationService adminApplicationService;

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
