package pl.edu.agh.backend.event.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.user.User;

public record AdminEventRegistrationResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID registrationId,

        @Schema(requiredMode = RequiredMode.REQUIRED) String displayName,
        @Schema(requiredMode = RequiredMode.REQUIRED) String email,

        @Schema(requiredMode = RequiredMode.REQUIRED) EventRegistrationStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "When the person signed up, seat or waitlist")
        Instant registeredAt,

        @Schema(description = "1-based place in the queue; present only for `WAITLISTED`")
        Integer waitlistPosition,

        @Schema(description = "User id for `/api/admin/alumni/{id}`; present only for verified alumni")
        UUID alumnusId) {

    public static AdminEventRegistrationResponse from(
            EventRegistration registration, Integer waitlistPosition, boolean alumnus) {
        User user = registration.getUser();
        return new AdminEventRegistrationResponse(
                registration.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                waitlistPosition,
                alumnus ? user.getId() : null);
    }
}
