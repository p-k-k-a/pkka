package pl.edu.agh.backend.event.message;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/events/{eventId}/messages")
@RequiredArgsConstructor
@Tag(name = "Admin Event Messages", description = "Messages from administrators to an event's sign-ups")
public class AdminEventMessageController {

    private final AdminEventMessageService adminEventMessageService;

    @GetMapping("/recipients")
    @Operation(summary = "Count who a message to this event would reach, waitlist included")
    @ApiResponse(responseCode = "200", description = "Number of recipients")
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public EventMessageRecipientsResponse countEventMessageRecipients(@PathVariable UUID eventId) {
        return adminEventMessageService.recipients(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Send a message to everyone signed up for an event, waitlist included",
            description = "Mailed to every recipient and pushed to those with the mobile app. Delivery happens in"
                    + " the background, so the response only says how many people it was queued for.")
    @ApiResponse(responseCode = "202", description = "Message queued")
    @ApiResponse(responseCode = "400", description = "Empty or too long subject or body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    public EventMessageRecipientsResponse sendEventMessage(
            @PathVariable UUID eventId, @Valid @RequestBody EventMessageRequest request) {
        return adminEventMessageService.send(eventId, request);
    }
}
