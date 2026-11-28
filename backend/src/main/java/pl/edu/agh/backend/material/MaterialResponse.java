package pl.edu.agh.backend.material;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.time.Instant;
import java.util.UUID;
import pl.edu.agh.backend.storage.StorageService;

public record MaterialResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        String description,
        @Schema(requiredMode = RequiredMode.REQUIRED) MaterialType type,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Signed, short-lived download URL when the material is an uploaded file, "
                        + "otherwise the external URL")
        String url,

        @Schema(
                description = "Extension of the uploaded file (e.g. `pdf`); null when the material is a link, "
                        + "so clients never have to tell the two apart from the URL")
        String fileExtension,

        UUID eventId,
        String eventTitle,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt) {

    /** The extension of an uploaded file's object key, {@code null} for a link. */
    static String fileExtension(String stored) {
        String key = StorageService.uploadedMaterialKey(stored);
        return key == null ? null : key.substring(key.lastIndexOf('.') + 1);
    }

    static MaterialResponse from(Material material, String url) {
        return new MaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getType(),
                url,
                fileExtension(material.getUrl()),
                material.getEvent() != null ? material.getEvent().getId() : null,
                material.getEvent() != null ? material.getEvent().getTitle() : null,
                material.getCreatedAt());
    }
}
