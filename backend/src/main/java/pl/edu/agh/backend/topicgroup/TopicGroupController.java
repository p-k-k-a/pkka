package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/topic-groups")
@RequiredArgsConstructor
@Tag(name = "Topic Groups", description = "Topic groups alumni join — requires VERIFIED_ALUMN role")
public class TopicGroupController {

    private final TopicGroupService topicGroupService;

    @GetMapping
    @Operation(summary = "List topic groups with their member count and whether the caller is a member (newest first)")
    public Page<TopicGroupResponse> listTopicGroups(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable, Caller caller) {
        return topicGroupService.list(caller, pageable);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a topic group by its slug, with its member count and the caller's place in it")
    @ApiResponse(responseCode = "200", description = "The group")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public TopicGroupResponse getTopicGroup(@PathVariable String slug, Caller caller) {
        return topicGroupService.get(caller, slug);
    }

    @PutMapping("/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Join a topic group", description = "Idempotent: joining a group one is in changes nothing.")
    @ApiResponse(responseCode = "204", description = "The caller is a member")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public void joinTopicGroup(@PathVariable UUID id, Caller caller) {
        topicGroupService.join(caller, id);
    }

    @DeleteMapping("/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Leave a topic group",
            description = "Idempotent: leaving a group one is not in changes nothing.")
    @ApiResponse(responseCode = "204", description = "The caller is not a member")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "The caller coordinates the group and must hand it over first",
            content = @Content)
    public void leaveTopicGroup(@PathVariable UUID id, Caller caller) {
        topicGroupService.leave(caller, id);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List the group's members in the order they joined")
    @ApiResponse(responseCode = "200", description = "The members")
    @ApiResponse(responseCode = "403", description = "The caller is neither a member nor an admin", content = @Content)
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public List<TopicGroupMemberResponse> listTopicGroupMembers(@PathVariable UUID id, Caller caller) {
        return topicGroupService.members(caller, id);
    }
}
