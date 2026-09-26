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
    @Operation(
            summary = "Register this device's push token",
            description = "Idempotent: call on every login and token change.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token stored"),
        @ApiResponse(
                responseCode = "400",
                description = "Missing token or platform",
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
    @Operation(summary = "Unregister this device", description = "Call on logout or when notifications are turned off.")
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
