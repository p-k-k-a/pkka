package pl.edu.agh.backend.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Alumnus applications submitted by the logged-in user")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit an alumnus application for the current user")
    @ApiResponse(responseCode = "201", description = "Application created")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "The user already has an application under review",
            content = @Content)
    public ApplicationResponse createApplication(@Valid @RequestBody CreateApplicationRequest request, Caller caller) {
        return applicationService.create(caller, request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user's application")
    @ApiResponse(responseCode = "200", description = "The user's most recent application")
    @ApiResponse(responseCode = "404", description = "The user has no application", content = @Content)
    public ApplicationResponse getMine(Caller caller) {
        return applicationService.getMine(caller);
    }
}
