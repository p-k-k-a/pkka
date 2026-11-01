package pl.edu.agh.backend.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.Comparator;
import java.util.UUID;

public record AlumniTagResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Alumni listed in the directory who have this tag; only in the tag catalog")
        Long usageCount) {

    public static final Comparator<AlumniTagResponse> MOST_USED_FIRST = Comparator.comparing(
                    AlumniTagResponse::usageCount, Comparator.reverseOrder())
            .thenComparing(AlumniTagResponse::name, String.CASE_INSENSITIVE_ORDER);

    public static AlumniTagResponse from(AlumniTag tag) {
        return new AlumniTagResponse(tag.getId(), tag.getName(), null);
    }
}
