package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import pl.edu.agh.backend.infrastructure.validation.TagNames;

@Schema(description = "Full replacement set: the union of tagIds and names")
public record UpdateTagsRequest(
        @Size(max = 20) @Schema(description = "IDs of existing tags")
        Set<UUID> tagIds,

        @Size(max = 20)
        @Schema(description = "Tag names, matched case-insensitively; names not in the catalog are created")
        Set<@NotBlank @Size(max = TagNames.MAX_LENGTH) String> names) {

    public UpdateTagsRequest {
        tagIds = tagIds == null ? Set.of() : tagIds;
        names = names == null
                ? Set.of()
                : names.stream().map(TagNames::normalize).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
