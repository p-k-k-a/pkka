package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/topic-proposals")
@RequiredArgsConstructor
@Tag(name = "Admin Topic Proposals", description = "Topic proposal moderation for administrators")
public class AdminTopicProposalController {

    private final AdminTopicProposalService adminTopicProposalService;

    @GetMapping
    @Operation(summary = "List all topic proposals, optionally filtered by status")
    public Page<AdminTopicProposalResponse> listTopicProposals(
            @RequestParam(required = false) TopicProposalStatus status,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminTopicProposalService.list(Optional.ofNullable(status), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single topic proposal")
    @ApiResponse(responseCode = "200", description = "Topic proposal details")
    @ApiResponse(responseCode = "404", description = "Topic proposal not found", content = @Content)
    public AdminTopicProposalResponse getTopicProposal(@PathVariable UUID id) {
        return adminTopicProposalService.get(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update the moderation status of a topic proposal")
    @ApiResponse(responseCode = "200", description = "Topic proposal updated")
    @ApiResponse(responseCode = "404", description = "Topic proposal not found", content = @Content)
    public AdminTopicProposalResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateTopicProposalStatusRequest request) {
        return adminTopicProposalService.updateStatus(id, request);
    }
}
