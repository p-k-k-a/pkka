package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.event.dto.EventDetailsResponse;
import pl.edu.agh.backend.event.dto.EventListItemResponse;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/public/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Public events calendar")
public class EventController {

    private final EventService eventService;

    @GetMapping
    @Operation(summary = "List of upcoming events (optional filtering by tags)")
    public Page<EventListItemResponse> listEvents(
            @RequestParam(required = false) Set<String> tags,
            @ParameterObject @PageableDefault(size = 20, sort = "startsAt") Pageable pageable,
            Caller caller) {
        return eventService.list(caller, tags, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Details of a single event")
    public EventDetailsResponse getEventById(@PathVariable UUID id, Caller caller) {
        return eventService.getDetails(id, caller);
    }
}
