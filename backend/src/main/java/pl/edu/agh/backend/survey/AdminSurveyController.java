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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/surveys")
@RequiredArgsConstructor
@Tag(name = "Admin Surveys", description = "Survey management for administrators")
public class AdminSurveyController {

    private final AdminSurveyService adminSurveyService;

    @GetMapping
    @Operation(summary = "List all surveys")
    public Page<AdminSurveyListItemResponse> listSurveys(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminSurveyService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single survey with questions")
    @ApiResponse(responseCode = "200", description = "Survey details")
    @ApiResponse(responseCode = "404", description = "Survey not found", content = @Content)
    public AdminSurveyResponse getSurvey(@PathVariable UUID id) {
        return adminSurveyService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a survey with questions")
    @ApiResponse(responseCode = "201", description = "Survey created")
    public AdminSurveyResponse createSurvey(@Valid @RequestBody CreateSurveyRequest request) {
        return adminSurveyService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a survey and replace its questions")
    @ApiResponse(responseCode = "200", description = "Survey updated")
    @ApiResponse(responseCode = "404", description = "Survey not found", content = @Content)
    public AdminSurveyResponse updateSurvey(@PathVariable UUID id, @Valid @RequestBody UpdateSurveyRequest request) {
        return adminSurveyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a survey permanently")
    @ApiResponse(responseCode = "204", description = "Survey deleted")
    @ApiResponse(responseCode = "404", description = "Survey not found", content = @Content)
    public void deleteSurvey(@PathVariable UUID id) {
        adminSurveyService.delete(id);
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Get aggregated survey results")
    @ApiResponse(responseCode = "200", description = "Survey results")
    @ApiResponse(responseCode = "404", description = "Survey not found", content = @Content)
    public SurveyResultsResponse getResults(@PathVariable UUID id) {
        return adminSurveyService.getResults(id);
    }
}
