package pl.edu.agh.backend.event.registration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.event.registration.dto.EventRegistrationResponse;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/events/{eventId}/registration")
@RequiredArgsConstructor
@Tag(
        name = "Event registrations",
        description = "Signing up for events — any signed-in user, limited by the event's audience")
public class EventRegistrationController {

    private final EventRegistrationService eventRegistrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign the current user up for an event", description = """
                    Takes one seat when the event has a free one, atomically with respect to other sign-ups
                    for the same event: an event with a seat limit can never be oversold. A full event puts
                    the caller on the waitlist instead of refusing them — the response says which happened in
                    `status`, and a queued sign-up also carries its `waitlistPosition`. Conflicts carry a
                    `reason` property (`ALREADY_REGISTERED`, `REGISTRATION_CLOSED`) so they can be told apart
                    without parsing the message. Registration closes at `registrationClosesAt`, and at the
                    event's start in any case; the waitlist closes with it.

                    Who may sign up follows the event's `audience`, exactly like who may see it: `PUBLIC`
                    events are open to every signed-in user, `ALL_ALUMNI` events to verified alumni only.
                    An event the caller cannot see reports 404 rather than 403.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Seat taken, or a place on the waitlist"),
        @ApiResponse(
                responseCode = "404",
                description = "No such event, or it is not visible to this user",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Already registered, or registration closed",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public EventRegistrationResponse register(@PathVariable UUID eventId, Caller caller) {
        return eventRegistrationService.register(eventId, caller);
    }

    @GetMapping
    @Operation(summary = "Get the current user's registration for an event", description = """
                    Says whether the caller holds a seat or is queuing for one, and for a queued sign-up how
                    many cancellations away from a seat they are, in `waitlistPosition`. A caller who is not
                    signed up gets 404, as does one who cannot see the event at all.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "The caller's own registration"),
        @ApiResponse(
                responseCode = "404",
                description = "No such event, or the caller is not signed up for it",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public EventRegistrationResponse getOwnRegistration(@PathVariable UUID eventId, Caller caller) {
        return eventRegistrationService.getOwnRegistration(eventId, caller);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel the current user's registration", description = """
                    Frees the seat. Before `registrationClosesAt` someone else can claim it; after that
                    nobody can, but dropping out still keeps the organiser's head count honest, so it stays
                    allowed. Once the event has started it achieves neither and is rejected with a `reason`
                    of `EVENT_ALREADY_STARTED`.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Seat freed"),
        @ApiResponse(
                responseCode = "404",
                description = "No such event, or the user is not registered for it",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "The event has already started",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void unregister(@PathVariable UUID eventId, Caller caller) {
        eventRegistrationService.unregister(eventId, caller);
    }
}
