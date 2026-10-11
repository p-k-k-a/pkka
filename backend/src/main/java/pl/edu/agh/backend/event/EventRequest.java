package pl.edu.agh.backend.event;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import pl.edu.agh.backend.infrastructure.validation.TagNames;

public record EventRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 200)
        String title,

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

        @Schema(description = "Minutes before the start to remind registered users; null sends no reminder")
        @Min(1)
        @Max(7 * 24 * 60)
        Integer reminderLeadTimeMinutes,

        @Schema(description = "Lower-cased on save; names not in the catalog are created")
        Set<@NotNull @Pattern(regexp = TagNames.PATTERN) String> tags) {

    public EventRequest {
        title = trimToNull(title);
        fullDescription = blankToNull(fullDescription);
        transmissionUrl = trimToNull(transmissionUrl);
        location = trimToNull(location);
        coverImageUrl = trimToNull(coverImageUrl);
        tags = tags == null
                ? new HashSet<>()
                : tags.stream()
                        .map(TagNames::normalize)
                        .map(TagNames::key)
                        .collect(Collectors.toCollection(HashSet::new));
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
