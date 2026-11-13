package pl.edu.agh.backend.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AvatarPresignRequest(
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Exact MIME type of the image: image/jpeg, image/png or image/webp",
                example = "image/webp")
        @NotBlank
        @Size(max = 100)
        String contentType,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Exact size of the image in bytes; the upload URL only accepts a body of this size")
        @NotNull
        @Positive
        Long sizeBytes) {}
