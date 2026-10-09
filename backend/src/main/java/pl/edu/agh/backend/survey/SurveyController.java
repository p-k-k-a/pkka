package pl.edu.agh.backend.survey;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/alumni/surveys")
@RequiredArgsConstructor
@Tag(name = "Surveys", description = "Active surveys available to verified alumni")
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping
    @Operation(summary = "List active surveys that have not yet ended")
    public Page<SurveyResponse> listActiveSurveys(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return surveyService.listActive(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active survey with questions")
    @ApiResponse(responseCode = "200", description = "Survey details")
    @ApiResponse(responseCode = "404", description = "Survey not found or still a draft", content = @Content)
    @ApiResponse(responseCode = "409", description = "Survey is closed or has ended", content = @Content)
    public SurveyResponse getActiveSurvey(@PathVariable UUID id) {
        return surveyService.getActive(id);
    }

    @PostMapping("/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit answers for an active survey")
    @ApiResponse(responseCode = "201", description = "Survey submitted")
    @ApiResponse(responseCode = "404", description = "Survey not found or still a draft", content = @Content)
    @ApiResponse(responseCode = "409", description = "Survey already submitted, closed or ended", content = @Content)
    public SubmitSurveyResponse submitSurvey(
            @PathVariable UUID id, @Valid @RequestBody SubmitSurveyRequest request, Caller caller) {
        return surveyService.submit(id, caller, request);
    }
}
