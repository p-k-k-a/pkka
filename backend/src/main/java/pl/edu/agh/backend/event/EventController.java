package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
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
    @Operation(summary = "List events (upcoming by default, optional past archive and tag filter)")
    public Page<EventListItemResponse> listEvents(
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false) EventTimeframe timeframe,
            @RequestParam(required = false) Audience audience,
            @ParameterObject @PageableDefault(size = 20, sort = "startsAt") Pageable pageable,
            Caller caller) {
        return eventService.list(caller, tags, timeframe, audience, pageable);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Details of a single event by its slug")
    public EventDetailsResponse getEventBySlug(@PathVariable String slug, Caller caller) {
        return eventService.getDetails(slug, caller);
    }
}
