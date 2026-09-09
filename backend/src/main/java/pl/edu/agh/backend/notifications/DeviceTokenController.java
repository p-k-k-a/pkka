package pl.edu.agh.backend.notifications;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import pl.edu.agh.backend.notifications.dto.RegisterDeviceRequest;
import pl.edu.agh.backend.security.Caller;

@RestController
@RequestMapping("/api/notifications/devices")
@RequiredArgsConstructor
@Tag(name = "Push devices", description = "Push tokens, one per app installation")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PutMapping("/{installationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register or update this installation's push token", description = """
                    The `installationId` is minted by the app on first launch and stays put; the push token
                    behind it is rotated by the push service and changes. Sending the same id again therefore
                    updates the token in place rather than adding a device, which makes this safe to call on
                    every login and on every token rotation.

                    A token already held by a different installation is released first — Android hands the same
                    token to a reinstalled app, and two rows holding it would push to that device twice.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token stored"),
        @ApiResponse(
                responseCode = "400",
                description = "Malformed push token",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void registerDevice(
            @PathVariable @Size(max = 64) String installationId,
            @Valid @RequestBody RegisterDeviceRequest request,
            Caller caller) {
        deviceTokenService.register(caller, installationId, request);
    }

    @DeleteMapping("/{installationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Forget this installation", description = """
                    Called on logout and when the user turns notifications off — the absence of a row is what
                    stops the sending, so there is no separate enabled flag to keep in step.
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Device forgotten"),
        @ApiResponse(
                responseCode = "404",
                description = "No such installation for this user",
                content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void unregisterDevice(@PathVariable @Size(max = 64) String installationId, Caller caller) {
        deviceTokenService.unregister(caller, installationId);
    }
}
