package pl.edu.agh.backend.topic;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/topic-proposals")
@RequiredArgsConstructor
@Tag(name = "Topic Proposals", description = "Topic proposals submitted by verified alumni")
public class TopicProposalController {

    private final TopicProposalService topicProposalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a new topic proposal")
    @ApiResponse(responseCode = "201", description = "Topic proposal created")
    public TopicProposalResponse createTopicProposal(
            @Valid @RequestBody CreateTopicProposalRequest request, Caller caller) {
        return topicProposalService.submit(caller, request);
    }

    @GetMapping
    @Operation(summary = "List topic proposals submitted by the current user")
    public Page<TopicProposalResponse> listMyTopicProposals(
            Caller caller, @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return topicProposalService.listMine(caller, pageable);
    }
}
