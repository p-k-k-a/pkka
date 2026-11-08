package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/topic-groups")
@RequiredArgsConstructor
@Tag(name = "Admin Topic Groups", description = "Club admin view of topic groups and their Discord channels")
public class AdminTopicGroupController {

    private final AdminTopicGroupService adminTopicGroupService;

    @GetMapping
    @Operation(summary = "List topic groups with their owner and Discord link (newest first)")
    public Page<AdminTopicGroupResponse> listAdminTopicGroups(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminTopicGroupService.list(pageable);
    }

    @PostMapping("/{id}/discord")
    @Operation(summary = "Create the group's missing Discord role or channel")
    @ApiResponse(responseCode = "200", description = "The group's Discord link after the attempt")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public AdminTopicGroupResponse linkTopicGroupDiscord(@PathVariable UUID id) {
        return adminTopicGroupService.linkDiscord(id);
    }
}
