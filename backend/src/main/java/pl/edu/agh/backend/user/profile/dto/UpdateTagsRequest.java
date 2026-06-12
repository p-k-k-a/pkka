package pl.edu.agh.backend.user.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record UpdateTagsRequest(
        @NotNull
        @Size(max = 20)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Full replacement set of tag IDs")
        Set<UUID> tagIds) {}
