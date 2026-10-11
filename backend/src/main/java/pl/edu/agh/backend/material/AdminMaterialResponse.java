package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;

public record AdminMaterialResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) MaterialType type,
        @Schema(requiredMode = RequiredMode.REQUIRED) String url,
        UUID eventId,
        String eventTitle,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminMaterialResponse from(Material material) {
        return new AdminMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getType(),
                material.getUrl(),
                material.getEvent() != null ? material.getEvent().getId() : null,
                material.getEvent() != null ? material.getEvent().getTitle() : null,
                material.getCreatedAt(),
                material.getUpdatedAt());
    }
}
