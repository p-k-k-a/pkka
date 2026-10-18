package pl.edu.agh.backend.event;

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
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(name = "Admin Events", description = "Event management for administrators")
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    @Operation(summary = "List all events including past and alumni-only, optionally filtered by timeframe")
    public Page<AdminEventSummaryResponse> listAdminEvents(
            @RequestParam(required = false) EventTimeframe timeframe,
            @ParameterObject @PageableDefault(size = 20, sort = "startsAt", direction = Direction.DESC)
                    Pageable pageable) {
        return adminEventService.list(timeframe == null ? EventTimeframe.ALL : timeframe, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single event regardless of audience or start date")
    @ApiResponse(responseCode = "200", description = "Event details")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public AdminEventResponse getAdminEvent(@PathVariable UUID id) {
        return adminEventService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an event")
    @ApiResponse(responseCode = "201", description = "Event created")
    public AdminEventResponse createAdminEvent(@Valid @RequestBody EventRequest request, Caller caller) {
        return adminEventService.create(caller, request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Replace an event's editable fields",
            description = "Full replacement: every editable field is taken from the body, "
                    + "so omitted optional fields are cleared.")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public AdminEventResponse updateAdminEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        return adminEventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete an event")
    @ApiResponse(responseCode = "204", description = "Event deleted")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public void deleteAdminEvent(@PathVariable UUID id) {
        adminEventService.delete(id);
    }
}
