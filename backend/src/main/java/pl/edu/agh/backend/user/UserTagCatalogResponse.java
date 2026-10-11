package pl.edu.agh.backend.user;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.UUID;

public record UserTagCatalogResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String name,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "Alumni listed in the directory who have this tag")
        long usageCount) {}
