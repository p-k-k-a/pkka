package pl.edu.agh.backend.event.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pl.edu.agh.backend.event.registration.EventRegistration;
import pl.edu.agh.backend.event.registration.EventRegistrationStatus;
import pl.edu.agh.backend.user.User;

public record AdminEventRegistrationResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID registrationId,
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID userId,

        @Schema(description = "First and last name; absent for a user who has not filled in either")
        String displayName,

        @Schema(description = "Absent for a user whose identity provider shared no address")
        String email,

        @Schema(requiredMode = RequiredMode.REQUIRED) EventRegistrationStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "When the person signed up, seat or waitlist")
        Instant registeredAt,

        @Schema(description = "1-based place in the queue; present only for `WAITLISTED`")
        Integer waitlistPosition) {

    public static AdminEventRegistrationResponse from(EventRegistration registration, Integer waitlistPosition) {
        User user = registration.getUser();
        String displayName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        return new AdminEventRegistrationResponse(
                registration.getId(),
                user.getId(),
                displayName.isEmpty() ? null : displayName,
                user.getEmail() == null || user.getEmail().isBlank() ? null : user.getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                waitlistPosition);
    }
}
