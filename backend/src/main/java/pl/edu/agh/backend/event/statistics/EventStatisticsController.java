package pl.edu.agh.backend.event.statistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.event.EventTimeframe;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(name = "Admin Event Statistics", description = "Sign-up statistics for administrators")
public class EventStatisticsController {

    private final EventStatisticsService eventStatisticsService;

    @GetMapping("/{id}/statistics")
    @Operation(
            summary = "Get sign-up statistics for one event",
            description = "Current seat and waitlist counts, cancellations, occupancy and sign-ups per day since"
                    + " publication. Cancellations are tracked from the moment this feature shipped; earlier ones"
                    + " were not recorded.")
    @ApiResponse(responseCode = "200", description = "Event statistics")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public EventStatisticsResponse getEventStatistics(@PathVariable UUID id) {
        return eventStatisticsService.getStatistics(id);
    }

    @GetMapping("/statistics")
    @Operation(
            summary = "Compare occupancy across events",
            description = "One row per event, newest start first, optionally limited to upcoming or past events.")
    public List<EventOccupancyResponse> listEventOccupancy(@RequestParam(required = false) EventTimeframe timeframe) {
        return eventStatisticsService.compareOccupancy(timeframe == null ? EventTimeframe.ALL : timeframe);
    }
}
