package pl.edu.agh.backend.topicgroup;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/admin/topic-group-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Topic Group Requests", description = "Reviewer actions on requests for new topic groups")
public class AdminTopicGroupRequestController {

    private final AdminTopicGroupRequestService adminTopicGroupRequestService;

    @GetMapping
    @Operation(summary = "List topic group requests filtered by status (newest first)")
    public Page<TopicGroupRequestResponse> listAdminTopicGroupRequests(
            @RequestParam(defaultValue = "UNDER_REVIEW") TopicGroupRequestStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminTopicGroupRequestService.list(status, pageable);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a topic group request and create the group")
    @ApiResponse(responseCode = "200", description = "Request approved")
    @ApiResponse(responseCode = "404", description = "Request not found", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Request is not under review, or a group with this name exists",
            content = @Content)
    public TopicGroupRequestResponse approveTopicGroupRequest(@PathVariable UUID id, Caller caller) {
        return adminTopicGroupRequestService.approve(caller, id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a topic group request with a reason")
    @ApiResponse(responseCode = "200", description = "Request rejected")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(responseCode = "404", description = "Request not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Request is not under review", content = @Content)
    public TopicGroupRequestResponse rejectTopicGroupRequest(
            @PathVariable UUID id, @Valid @RequestBody RejectTopicGroupRequest request, Caller caller) {
        return adminTopicGroupRequestService.reject(caller, id, request.reason());
    }
}
