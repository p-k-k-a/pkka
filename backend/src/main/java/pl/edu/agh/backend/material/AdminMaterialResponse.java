package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.event.Audience;
import pl.edu.agh.backend.storage.StorageService;

public record AdminMaterialResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) MaterialType type,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Signed, short-lived download URL when the material is an uploaded file, "
                        + "otherwise the external URL; sending it back unchanged on update is fine")
        String url,

        @Schema(
                description = "Extension of the uploaded file (e.g. `pdf`); null when the material is a link, "
                        + "so clients never have to tell the two apart from the URL")
        String fileExtension,

        @Schema(
                description = "Object key of the uploaded file, to send back as `url` when the file stays; null "
                        + "for a link")
        String fileKey,

        @Schema(description = "The linked event; still set after that event was deleted, see `eventDeleted`")
        UUID eventId,

        @Schema(description = "Title of the linked event; null when there is none or it was deleted")
        String eventTitle,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "The linked event was deleted: the material is hidden from everyone but admins until "
                        + "it gets another event or none. Sending `eventId` back unchanged keeps it that way")
        boolean eventDeleted,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Who may see the material while it has no linked event; a linked event decides instead")
        Audience audience,

        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant updatedAt) {

    static AdminMaterialResponse from(Material material, String url) {
        return new AdminMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getType(),
                url,
                MaterialResponse.fileExtension(material.getUrl()),
                StorageService.uploadedMaterialKey(material.getUrl()),
                material.getEventId(),
                material.getEvent() != null ? material.getEvent().getTitle() : null,
                material.getEventId() != null && material.getEvent() == null,
                material.getAudience(),
                material.getCreatedAt(),
                material.getUpdatedAt());
    }
}
