package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import pl.edu.agh.backend.infrastructure.validation.TagNames;

@Schema(description = "Full replacement set of own tags")
public record UpdateTagsRequest(
        @NotNull
        @Size(max = TagNames.MAX_PER_USER)
        @Schema(description = "Tag names, matched case-insensitively; names not in the catalog are created")
        Set<@NotBlank @Size(max = TagNames.MAX_LENGTH) String> names) {

    public UpdateTagsRequest {
        if (names != null) {
            names = names.stream().map(TagNames::normalize).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
