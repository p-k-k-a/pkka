package pl.edu.agh.backend.storage;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminPresignUploadRequest(
        @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
        AdminUploadPurpose purpose,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Exact MIME type of the file, without parameters",
                example = "application/pdf")
        @NotBlank
        @Size(max = 100)
        String contentType,

        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                description = "Exact size of the file in bytes; the upload URL only accepts a body of this size")
        @NotNull
        @Positive
        Long sizeBytes) {}
