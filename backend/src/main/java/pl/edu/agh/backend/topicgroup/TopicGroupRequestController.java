package pl.edu.agh.backend.topicgroup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/me/topic-group-requests")
@RequiredArgsConstructor
@Tag(name = "Topic Group Requests", description = "Requests for new topic groups made by the logged-in alumn")
public class TopicGroupRequestController {

    private final TopicGroupRequestService topicGroupRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ask the admins to create a new topic group")
    @ApiResponse(responseCode = "201", description = "Request submitted")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "The user already has a request under review, or a group with this name exists",
            content = @Content)
    public TopicGroupRequestResponse submitTopicGroupRequest(
            @Valid @RequestBody SubmitTopicGroupRequest request, Caller caller) {
        return topicGroupRequestService.submit(caller, request);
    }

    @GetMapping
    @Operation(summary = "List the current user's topic group requests (newest first)")
    public List<TopicGroupRequestResponse> listMyTopicGroupRequests(Caller caller) {
        return topicGroupRequestService.listMine(caller);
    }
}
