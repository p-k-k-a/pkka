package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public record EventRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 200)
        String title,

        @Size(max = 400) String shortDescription,

        String fullDescription,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        EventType type,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant startsAt,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Instant endsAt,

        @Size(max = 500) String transmissionUrl,
        @Size(max = 300) String location,
        @Min(0) Integer seatLimit,
        Instant registrationClosesAt,

        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        Audience audience,

        @Size(max = 500) String coverImageUrl,
        Set<@NotBlank @Size(max = 32) String> tags) {

    public EventRequest {
        title = trimToNull(title);
        shortDescription = trimToNull(shortDescription);
        fullDescription = blankToNull(fullDescription);
        transmissionUrl = trimToNull(transmissionUrl);
        location = trimToNull(location);
        coverImageUrl = trimToNull(coverImageUrl);
        tags = tags == null || tags.isEmpty() ? new HashSet<>() : new HashSet<>(tags);
    }

    @AssertTrue(message = "endsAt must be after startsAt")
    @Schema(hidden = true)
    public boolean isPeriodValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }

    @AssertTrue(message = "registrationClosesAt must not be after startsAt")
    @Schema(hidden = true)
    public boolean isRegistrationWindowValid() {
        return registrationClosesAt == null || startsAt == null || !registrationClosesAt.isAfter(startsAt);
    }

    /** For single-line fields, where surrounding whitespace is always accidental. */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * For rich text, which is stored verbatim: blank lines around an embedded image or attachment are
     * the author's layout, so only an entirely blank body is discarded.
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
