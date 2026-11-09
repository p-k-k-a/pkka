package pl.edu.agh.backend.event.registration;

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
import org.springframework.web.bind.annotation.RestController;
import pl.edu.agh.backend.event.registration.dto.AdminEventRegistrationResponse;

@RestController
@RequestMapping("/api/admin/events/{eventId}/registrations")
@RequiredArgsConstructor
@Tag(name = "Admin Event Registrations", description = "Who signed up for an event, for administrators")
public class AdminEventRegistrationController {

    private final AdminEventRegistrationService adminEventRegistrationService;

    @GetMapping
    @Operation(
            summary = "List everyone signed up for an event",
            description = "Seat holders first, in the order they signed up, then the waitlist in the order it will"
                    + " be promoted, each with its `waitlistPosition`. Cancelled sign-ups are not listed.")
    @ApiResponse(responseCode = "200", description = "The event's sign-ups")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public List<AdminEventRegistrationResponse> listEventRegistrations(@PathVariable UUID eventId) {
        return adminEventRegistrationService.list(eventId);
    }
}
